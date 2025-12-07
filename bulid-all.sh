#!/bin/bash

echo "=== Docker 환경 설정 ==="
eval $(minikube docker-env)

echo "=== Employee Service 빌드 ==="
cd employee-service
./gradlew clean build -x test
docker build -t employee-service:latest .
cd ..

echo "=== Approval Request Service 빌드 ==="
cd approval-request-service
./gradlew clean build -x test
docker build -t approval-request-service:latest .
cd ..

echo "=== Approval Processing Service 빌드 ==="
cd approval-processing-service
./gradlew clean build -x test
docker build -t approval-processing-service:latest .
cd ..

echo "=== Notification Service 빌드 ==="
cd notification-service
./gradlew clean build -x test
docker build -t notification-service:latest .
cd ..

echo "=== 빌드 완료 ==="
docker images | grep -E "employee|approval|notification"