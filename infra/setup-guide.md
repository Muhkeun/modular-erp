# ModularERP — AWS 배포 가이드

## 사전 준비

### 1. 개인 AWS 프로필 설정 (로컬)
```bash
aws configure --profile personal
# AWS Access Key ID: <개인 계정 키>
# AWS Secret Access Key: <개인 계정 시크릿>
# Default region: ap-northeast-2
# Default output format: json
```

### 2. AWS 리소스 생성 (개인 계정)

#### ECR 리포지토리
```bash
aws ecr create-repository --repository-name modular-erp --profile personal --region ap-northeast-2
```

#### EC2 인스턴스
- AMI: Amazon Linux 2023
- Instance Type: t3.small (2 vCPU, 2GB RAM)
- Storage: 20GB gp3
- Security Group: 80(HTTP), 443(HTTPS), 22(SSH) 오픈
- Key Pair: 새로 생성 또는 기존 키 사용
- IAM Role: EC2에 ECR 읽기 권한 부여 (`AmazonEC2ContainerRegistryReadOnly`)

#### EC2 초기 설정
```bash
ssh ec2-user@<EC2_PUBLIC_IP>
# ec2-setup.sh 내용을 실행
```

### 3. GitHub Secrets 등록

GitHub repo → Settings → Secrets and variables → Actions에 추가:

| Secret | 값 |
|--------|---|
| `AWS_ACCESS_KEY_ID` | 개인 AWS IAM Access Key |
| `AWS_SECRET_ACCESS_KEY` | 개인 AWS Secret Key |
| `EC2_HOST` | EC2 Public IP 또는 도메인 |
| `EC2_SSH_KEY` | EC2 키페어의 private key 전체 내용 |

### 4. Route53 도메인 연결

```bash
# A 레코드 추가: erp.yourdomain.com → EC2 Public IP
aws route53 change-resource-record-sets \
  --hosted-zone-id <ZONE_ID> \
  --change-batch '{
    "Changes": [{
      "Action": "UPSERT",
      "ResourceRecordSet": {
        "Name": "erp.yourdomain.com",
        "Type": "A",
        "TTL": 300,
        "ResourceRecords": [{"Value": "<EC2_PUBLIC_IP>"}]
      }
    }]
  }' --profile personal
```

## 배포 흐름

```
git push main
    │
    ▼
GitHub Actions (.github/workflows/deploy.yml)
    │
    ├─ Docker build (backend + frontend)
    ├─ Push to ECR
    └─ SSH to EC2 → docker compose pull → up -d
```

## 배포 후 확인

```bash
# EC2에서 확인
ssh ec2-user@<EC2_HOST>
cd modular-erp
docker compose ps
docker compose logs app --tail 50

# 브라우저에서 확인
# http://erp.yourdomain.com
# http://erp.yourdomain.com/swagger-ui.html
```
