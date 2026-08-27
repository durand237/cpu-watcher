#!/bin/bash
set -euxo pipefail

dnf update -y
dnf install -y docker git
systemctl enable --now docker
usermod -aG docker ec2-user

# Application deployment is intentionally a later step. This directory is the
# future home for the production Docker Compose files and environment settings.
install -d -o ec2-user -g ec2-user /opt/cpu-watcher
