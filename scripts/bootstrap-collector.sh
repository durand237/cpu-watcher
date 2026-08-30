#!/usr/bin/env bash
set -euo pipefail

for required_var in AWS_REGION PARAMETER_PREFIX; do
  : "${!required_var:?$required_var must be set}"
done

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
collector_dir="$script_dir/../collector"

test -d "$collector_dir"

# The deployment command runs as root through SSM. Install the small Java
# runtime dependency only when the original EC2 bootstrap did not do so.
if ! command -v java >/dev/null 2>&1; then
  dnf install -y java-17-amazon-corretto-headless
fi

if ! getent group cpu-watcher >/dev/null 2>&1; then
  groupadd --system cpu-watcher
fi

if ! id --user cpu-watcher >/dev/null 2>&1; then
  useradd --system --gid cpu-watcher --home-dir /opt/cpu-watcher --shell /sbin/nologin cpu-watcher
fi

cd "$collector_dir"
chmod +x gradlew
# Keep the one-time host build within the memory budget of the t3.micro that
# also runs PostgreSQL and the API. A single worker is sufficient here.
./gradlew --no-daemon --max-workers=1 -Dorg.gradle.jvmargs="-Xmx256m -XX:MaxMetaspaceSize=192m" bootJar

collector_jar="$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print -quit)"
test -n "$collector_jar"

install -d -o root -g cpu-watcher -m 0750 /opt/cpu-watcher
install -o root -g cpu-watcher -m 0550 "$collector_jar" /opt/cpu-watcher/collector.jar

cat >/usr/local/bin/cpu-watcher-collector <<'EOF'
#!/usr/bin/env bash
set -euo pipefail

collector_api_key="$(aws ssm get-parameter \
  --region "$AWS_REGION" \
  --name "$COLLECTOR_API_KEY_PARAMETER_NAME" \
  --with-decryption \
  --query 'Parameter.Value' \
  --output text)"

export COLLECTOR_API_KEY="$collector_api_key"
exec /usr/bin/java -jar /opt/cpu-watcher/collector.jar
EOF
chmod 0755 /usr/local/bin/cpu-watcher-collector

cat >/etc/systemd/system/cpu-watcher-collector.service <<EOF
[Unit]
Description=CPU Watcher host collector
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=cpu-watcher
Group=cpu-watcher
Environment=AWS_REGION=$AWS_REGION
Environment=COLLECTOR_API_KEY_PARAMETER_NAME=/$PARAMETER_PREFIX/collector/api-key
Environment=COLLECTOR_BACKEND_URL=http://127.0.0.1:8080
ExecStart=/usr/local/bin/cpu-watcher-collector
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable --now cpu-watcher-collector
