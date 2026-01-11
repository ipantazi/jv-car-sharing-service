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

    docker build -t bookstore-api .
    docker tag bookstore-api:latest YOUR_AWS_ID.dkr.ecr.eu-north-1.amazonaws.com/bookstore-api:v01

### Authenticate and Push to ECR

    aws ecr get-login-password --region eu-north-1 | \
    docker login --username AWS --password-stdin YOUR_AWS_ID.dkr.ecr.eu-north-1.amazonaws.com

    docker push YOUR_AWS_ID.dkr.ecr.eu-north-1.amazonaws.com/bookstore-api:v01

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

3. Set DB name: `online_book_store`

4. Create user: `admin`, password: `YOUR_PASSWORD`

5. Make RDS publicly accessible and open port 3306 in security group

6. Ensure EC2 and RDS are in the same VPC

## 🚀 4. Run App on EC2

    docker pull YOUR_AWS_ID.dkr.ecr.eu-north-1.amazonaws.com/bookstore-api:v01

    docker run --name bookstore-api -d -p 80:8080 \
    -e SPRING_DATASOURCE_URL=jdbc:mysql://<RDS-ENDPOINT>:3306/online_book_store \
    -e SPRING_DATASOURCE_USERNAME=admin \
    -e SPRING_DATASOURCE_PASSWORD=YOUR_PASSWORD \
    YOUR_AWS_ID.dkr.ecr.eu-north-1.amazonaws.com/bookstore-api:v01

Replace `<RDS-ENDPOINT>` with the hostname shown in the RDS dashboard (e.g., `mydb.xxxxx.eu-north-1.rds.amazonaws.com`).

## 🌐 5. Access the Application

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
