# ☁️ Deployment Guide: jv-car-sharing-service

This guide explains how to deploy the application to Amazon Web Services (AWS) using EC2 (compute), 
RDS (database), and ECR (Docker image repository). It also outlines CI/CD basics for automated deployment.

## 🔧 Prerequisites

- AWS account (Free Tier is sufficient)

- AWS CLI installed and configured: `aws configure`

- Docker installed locally

- Maven build: `mvn clean package`

## 🐳 1. Build & Push Docker Image to ECR

### Build and Tag the Image

    docker build -t carsharing-api .
    docker tag carsharing-api:latest YOUR_AWS_ID.dkr.ecr.eu-north-1.amazonaws.com/bookstore-api:v01

### Authenticate and Push to ECR

    aws ecr get-login-password --region eu-north-1 | \
    docker login --username AWS --password-stdin YOUR_AWS_ID.dkr.ecr.eu-north-1.amazonaws.com

    docker push YOUR_AWS_ID.dkr.ecr.eu-north-1.amazonaws.com/carsharing-api:v01

Replace `YOUR_AWS_ID` with your actual AWS account ID (12-digit number).

## 🖥️ 2. Launch EC2 Instance

1. Go to EC2 → Launch Instance

2. Choose **Amazon Linux 2 AMI**

3. Create a **key pair** (for SSH)

4. Open port **80** in security group

5. SSH into instance:


    ssh -i "your-key.pem" ec2-user@ec2-YOUR_PUBLIC_IP.compute-1.amazonaws.com

6. Install Docker on EC2


    sudo yum update -y
    sudo yum install -y docker
    sudo service docker start
    sudo usermod -a -G docker ec2-user

You may need to logout/login or run `newgrp docker` for permissions to apply.

## 🗄️ 3. Set Up RDS (MySQL)

1. Go to RDS → Create database

2. Select MySQL, Free Tier

3. Set DB name: `car_sharing_service`

4. Create user: `admin`, password: `YOUR_PASSWORD`

5. Make RDS publicly accessible and open port 3306 in security group

6. Ensure EC2 and RDS are in the same VPC

## 🔐 4. Environment Variables on EC2 (app.env)

For security and flexibility, sensitive configuration (database credentials, **JWT secret**, **Stripe keys**, etc.) must not 
be hardcoded into the **Docker image** or committed to the repository.
On **AWS EC2**, this is handled using an environment file (`app.env`), which is functionally similar to **GitHub Actions
secrets**.

### 4.1 Create `app.env` on EC2

After connecting to the **EC2 instance** via SSH:
```bash
cd /home/ec2-user
nano app.env
```

Add the required environment variables:
```bash
# Database (Amazon RDS)
MYSQLDB_USER=admin
MYSQLDB_PASSWORD=YOUR_RDS_PASSWORD
MYSQLDB_DATABASE=car_sharing_service

# Security
JWT_SECRET=your_jwt_secret_here

# Stripe
STRIPE_SECRET_KEY=sk_test_****
STRIPE_WEBHOOK_SECRET=whsec_****

# Telegram
TELEGRAM_BOT_TOKEN=****
TELEGRAM_CHAT_ID=****
```
### 4.2 Secure the file

Restrict access so only the **EC2 user** can read it:

```bash
chmod 600 /home/ec2-user/app.env
```

This prevents accidental exposure of secrets.

## 🚀 5. Run the application using app.env

Pull the image from **ECR**:

```bash
docker pull YOUR_AWS_ID.dkr.ecr.eu-north-1.amazonaws.com/carsharing-api:v01
```

Run the container with environment variables loaded from `app.env`:

```bash
docker run -d --name carsharing-api \
--env-file /home/ec2-user/app.env \
-p 80:8080 \
YOUR_AWS_ID.dkr.ecr.eu-north-1.amazonaws.com/carsharing-api:v01
```

Docker will automatically inject all variables from `app.env` into the container, and **Spring Boot** will resolve them 
via `${VAR_NAME}`.

## 🌐 6. Access the Application

Open in browser:

    http://ec2-YOUR_PUBLIC_IP.compute-1.amazonaws.com/api/swagger-ui/index.html

## 🔁 Optional: CI/CD

### You can automate builds and deployment using:

- GitHub Actions / Jenkins / AWS CodePipeline

- Typical workflow:

    1. `mvn package`

    2. Docker build/tag

    3. Push to ECR

    4. Deploy remotely to EC2 via SSH or SSM

This helps deliver updates faster and more reliably.

## ✅ Notes

* Use `sudo docker ps` to check if the container is running

* Make sure security groups allow traffic on ports 80 (EC2) and 3306 (RDS)

* Your key pair (`.pem` file) must be stored safely to access EC2 via SSH
