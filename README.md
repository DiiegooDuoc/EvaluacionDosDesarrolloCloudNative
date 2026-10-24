# Sistema de Gestión de Pedidos - Mensajería Asíncrona con RabbitMQ

Evolución del Sistema de Gestión de Pedidos incorporando mensajería asíncrona desacoplada mediante un clúster de RabbitMQ de dos nodos en Docker, exchanges (`direct` y `topic`), tres colas principales con sus respectivas Dead Letter Queues (`DLQ`), confirmación manual (`ACK` / `NACK`), microservicio de administración de RabbitMQ y cliente web en React con autenticación OAuth 2.0 / OpenID Connect en Microsoft Entra ID.

---

## Estructura del Repositorio

* `docker-compose.yml`: Configuración del clúster de 2 nodos de RabbitMQ (`rabbit-node-1` y `rabbit-node-2`).
* `spring-resource-server`: Microservicio principal (puerto `8080`) con gestión de pedidos, productores, consumidores `@RabbitListener` agrupados por dominio funcional y administración de colas.
* `ms-rabbit-admin`: Microservicio administrador de RabbitMQ (puerto `8081`) para creación y eliminación de colas, exchanges y bindings.
* `react-msal`: Frontend SPA en React + TypeScript con integración MSAL (Microsoft Entra ID).

---

## Ejecución del Proyecto

### 1. Clúster RabbitMQ (Docker Compose)

```bash
docker-compose up -d
```

* Panel de administración Nodo 1: `http://localhost:15672` (`guest` / `guest`)
* Panel de administración Nodo 2: `http://localhost:15673` (`guest` / `guest`)

### 2. Backend (Spring Boot)

```bash
cd spring-resource-server
mvn clean spring-boot:run
```

*(Opcional) Para levantar el microservicio administrador independiente en el puerto 8081:*
```bash
cd ms-rabbit-admin
mvn clean spring-boot:run
```

### 3. Frontend (React + MSAL)

```bash
cd react-msal
npm install
npm run dev
```

La aplicación web estará disponible en `http://localhost:5173`.
