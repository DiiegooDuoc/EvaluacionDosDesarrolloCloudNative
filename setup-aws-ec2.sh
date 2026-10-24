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

echo "Esperando a que rabbit-node-2 inicie para unirlo al cluster..."
until sudo docker exec rabbit-node-2 rabbitmq-diagnostics -q ping >/dev/null 2>&1; do
    sleep 2
done

sudo docker exec rabbit-node-2 rabbitmqctl stop_app
sudo docker exec rabbit-node-2 rabbitmqctl reset
sudo docker exec rabbit-node-2 rabbitmqctl join_cluster rabbit@rabbit-node-1
sudo docker exec rabbit-node-2 rabbitmqctl start_app
sudo docker exec rabbit-node-1 rabbitmqctl cluster_status

echo "=========================================================="
echo " Listo! El cluster de 2 nodos RabbitMQ esta activo en 15672"
echo "=========================================================="
