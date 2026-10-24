#!/bin/bash
set -e

echo "=========================================================="
echo " Instalando dependencias en Ubuntu / Amazon Linux (EC2)"
echo "=========================================================="

if command -v apt-get >/dev/null 2>&1; then
    sudo apt-get update -y
    sudo apt-get install -y docker.io docker-compose openjdk-17-jdk maven nodejs npm git
    sudo systemctl enable --now docker
    sudo usermod -aG docker $USER || true
elif command -v dnf >/dev/null 2>&1; then
    sudo dnf update -y
    sudo dnf install -y docker java-17-amazon-corretto-devel maven nodejs npm git
    sudo systemctl enable --now docker
    sudo usermod -aG docker $USER || true
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
elif command -v yum >/dev/null 2>&1; then
    sudo yum update -y
    sudo yum install -y docker java-17-amazon-corretto-devel maven git
    sudo systemctl enable --now docker
    sudo usermod -aG docker $USER || true
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
fi

echo "=========================================================="
echo " Levantando Cluster RabbitMQ de 2 Nodos con Docker Compose"
echo "=========================================================="
sudo docker-compose up -d || sudo docker compose up -d

echo "Esperando 15 segundos a que los 2 nodos de RabbitMQ formen el cluster..."
sleep 15
sudo docker exec rabbit-node-1 rabbitmqctl cluster_status || true

echo "=========================================================="
echo " Listo! El cluster RabbitMQ esta corriendo en el puerto 15672"
echo "=========================================================="
