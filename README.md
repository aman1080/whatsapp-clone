# WhatsApp-like Chat Foundation

Starter project: Angular + Spring Boot + STOMP/WebSocket + JWT + MySQL.

## Prerequisites
- Java 21
- Maven 3.9+
- Node.js 20+ and npm
- MySQL 8+

## 1. Start MySQL
Use local MySQL or run the included container:
```bash
docker compose up -d mysql
```
The container creates `whatsapp_clone` automatically. If you use local MySQL, create it with `CREATE DATABASE whatsapp_clone CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;`.
Update `backend/src/main/resources/application.yml` with your MySQL username/password. For local development the sample uses `root` / `root`; change it before running.

## 2. Run backend
```bash
cd backend
mvn spring-boot:run
```
Backend: `http://localhost:8080`

## 3. Run Angular frontend
```bash
cd frontend
npm install
npm start
```
Open `http://localhost:4200`.

## 4. Try it
1. Register two accounts in separate browsers/incognito windows.
2. Log in as each user.
3. Use the contacts panel to start a private chat, or create a group and add members.
4. Open the same chat from both accounts and exchange messages.

## Main API
- `POST /api/auth/register` `{ "displayName":"Alice", "email":"alice@example.com", "password":"Password123!" }`
- `POST /api/auth/login` `{ "email":"alice@example.com", "password":"Password123!" }`
- `GET /api/users`
- `GET /api/chats`
- `POST /api/chats/private` `{ "otherUserId": 2 }`
- `POST /api/chats/group` `{ "name":"Team", "memberIds":[2,3] }`
- `GET /api/chats/{chatId}/messages?limit=100`

STOMP endpoint: `/ws` (SockJS enabled)
- Publish: `/app/chat.send` with `{ "chatId": 1, "content": "Hello" }`
- Private receive: `/user/queue/messages`
- Group receive: `/topic/chat/{chatId}`

## Important security notes
- JWT is checked on REST requests and on STOMP `CONNECT` frames.
- The backend derives sender identity from the authenticated principal; clients cannot choose a sender ID.
- Group subscriptions are authorized against database membership before subscription is accepted.
- This is a learning foundation, not a production-ready WhatsApp replacement. Use HTTPS/WSS, rotate secrets, rate-limit auth, add refresh-token/revocation strategy, pagination, malware scanning for uploads, monitoring, and proper migrations before production.
- JWT signing key is supplied by `JWT_SECRET`. The sample default is for local development only.
