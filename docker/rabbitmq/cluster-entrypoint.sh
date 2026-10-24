#!/bin/bash
set -e

# Iniciar servidor RabbitMQ en segundo plano para unirlo al nodo 1
/usr/local/bin/docker-entrypoint.sh rabbitmq-server -detached

echo "Esperando a que el nodo local rabbit@rabbit-node-2 inicie..."
until rabbitmqctl node_health_check >/dev/null 2>&1; do
  sleep 2
done

echo "Esperando a que el nodo principal rabbit@rabbit-node-1 este disponible..."
until rabbitmqctl -n rabbit@rabbit-node-1 node_health_check >/dev/null 2>&1; do
  sleep 2
done

echo "Uniendo rabbit@rabbit-node-2 al cluster con rabbit@rabbit-node-1..."
rabbitmqctl stop_app
rabbitmqctl reset
rabbitmqctl join_cluster rabbit@rabbit-node-1
rabbitmqctl start_app

echo "Cluster configurado exitosamente. Estado del cluster:"
rabbitmqctl cluster_status

# Mantener el contenedor activo siguiendo los logs
tail -f /var/log/rabbitmq/*.log 2>/dev/null || sleep infinity
