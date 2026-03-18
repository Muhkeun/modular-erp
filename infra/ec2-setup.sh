#!/bin/bash
# EC2 초기 세팅 스크립트 (Amazon Linux 2023)
# SSH 접속 후 한 번만 실행하면 됩니다.

set -e

echo "=== Installing Docker ==="
sudo yum update -y
sudo yum install -y docker
sudo systemctl enable docker
sudo systemctl start docker
sudo usermod -aG docker ec2-user

echo "=== Installing Docker Compose ==="
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
sudo ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose

echo "=== Installing AWS CLI v2 ==="
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip -q awscliv2.zip
sudo ./aws/install --update
rm -rf aws awscliv2.zip

echo "=== Creating app directory ==="
mkdir -p /home/ec2-user/modular-erp
cat > /home/ec2-user/modular-erp/docker-compose.yml << 'COMPOSE'
version: "3.8"

services:
  db:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: modularerp
      POSTGRES_USER: modularerp
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U modularerp"]
      interval: 5s
      timeout: 5s
      retries: 5

  app:
    image: ${ECR_IMAGE}
    depends_on:
      db:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: postgres
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/modularerp
      DB_USERNAME: modularerp
      DB_PASSWORD: ${DB_PASSWORD}
      MODULAR_ERP_SECURITY_JWT_SECRET: ${JWT_SECRET}
    ports:
      - "80:8080"
    restart: unless-stopped

volumes:
  pgdata:
COMPOSE

# .env 파일 생성 (배포 시 GitHub Actions가 ECR_IMAGE를 주입)
cat > /home/ec2-user/modular-erp/.env << 'ENV'
DB_PASSWORD=change-this-strong-password
JWT_SECRET=change-this-must-be-at-least-32-bytes-long!!
ECR_IMAGE=PLACEHOLDER
ENV

echo ""
echo "=== Setup complete! ==="
echo "Next steps:"
echo "1. Edit /home/ec2-user/modular-erp/.env with real passwords"
echo "2. Re-login (for docker group): exit && ssh ec2-user@<host>"
echo "3. GitHub Actions will handle deployment from here"
