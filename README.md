# GoatSports Notification Service

Hộp thư thông báo và đẩy thông báo realtime tới web / mobile qua WebSocket (STOMP).

## Chức năng chính

- **Hộp thư**: danh sách, đánh dấu đã đọc, số chưa đọc; `/notifications/since` để client lấy phần bị lỡ khi kết nối lại.
- **Đẩy realtime** qua STOMP. `SubscriptionGuard` chỉ cho người dùng subscribe topic của chính mình
  (topic admin chỉ cho ADMIN). Kết nối `/ws` qua gateway.
- **Thu sự kiện từ các service** qua Kafka: thanh toán, CLB / giải đấu, cộng đồng; khử trùng theo `eventId`.
- **Thông báo nội bộ** (`/internal/notifications`) cho service gọi trực tiếp, ví dụ đặt sân, ghép trận,
  **đá phiên đăng nhập** khi tài khoản bị khóa (`SESSION_REVOKED`).
- **Gửi thông báo hàng loạt** từ admin (`/api/v1/admin/broadcasts`), phát dần bởi `BroadcastDeliveryWorker`.

## API chính

| Đường dẫn | Mô tả |
|---|---|
| `/api/v1/notifications` | Hộp thư của người dùng |
| `/api/v1/admin/broadcasts` | Thông báo hàng loạt |
| `/internal/notifications`, `/internal/users` | API nội bộ (cần `X-Gateway-Secret`) |
| `/ws` | Endpoint STOMP |

## Cấu trúc

```
src/main/java/iuh/fit/notificationservice/
├── domain/          # entity, enum
├── application/     # service, port, DTO, mapper
├── infrastructure/  # JPA, Kafka consumer, WebSocket, Feign
└── presentation/    # REST controller
shared-kernel/       # @AuditLog, Feign interceptor dùng chung
```

## Tích hợp

- **PostgreSQL** (Liquibase).
- **Kafka nhận**: `payment.events`, `club.events`, `social.events`, `user-deleted`. **Kafka phát**: `user-log`.
- **Feign**: auth-service.

## Chạy và kiểm thử

```bash
mvn -f shared-kernel/pom.xml clean install -DskipTests
mvn spring-boot:run -Dspring-boot.run.profiles=dev     # cần application-dev.properties (bị .gitignore)
mvn test
```

## CI/CD

`.gitlab-ci.yml`: test → `jib:build` lên Docker Hub → gọi Jenkins job `GoatSports-CD`.
