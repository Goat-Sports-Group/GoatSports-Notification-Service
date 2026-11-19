package iuh.fit.notificationservice.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Hộp thư thông báo mẫu (dev / demo) cho tài khoản demo — người chơi, chủ sân và admin kiểm duyệt — đủ các loại
 * (đặt sân, thanh toán, ghép kèo, giải đấu, CLB, kết bạn, hoàn tiền, đánh giá, kiểm duyệt...) cùng 3 thông báo hàng
 * loạt admin đã gửi. Thông báo trỏ tới giải đấu dùng đúng id giải mô phỏng; loại không có id cụ thể thì để trống để giao
 * diện mở trang danh sách tương ứng.
 *
 * <p>Id người dùng theo cùng quy ước với auth-service ({@code goatsports:user:<email>}). Chạy một lần mỗi người (bỏ qua
 * nếu người đó đã có thông báo mẫu): đọc / xóa lúc demo không bị khởi động lại ghi đè.
 * Bật bằng {@code app.demo-data.simulation.enabled=true}.
 */
@Slf4j
@Component
@Order(22)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.demo-data.simulation", name = "enabled", havingValue = "true")
public class DemoNotificationSeeder implements CommandLineRunner {
    private static final String MODERATOR = "kiemduyet@goatsports.vn";
    private static final List<String> PLAYERS = List.of("huy.danggia@goatsports.vn", "tung.vuthanh@goatsports.vn",
            "minh.hoangnhat@goatsports.vn", "linh.buikhanh@goatsports.vn", "anh.ngoduc@goatsports.vn",
            "dang.trinhhai@goatsports.vn");
    private static final List<String> OWNERS = List.of("bao.tranquoc@goatsports.vn", "huong.nguyenthimai@goatsports.vn",
            "phuc.lehoang@goatsports.vn", "khoi.phamminh@goatsports.vn");

    private final JdbcTemplate jdbc;

    /** Một thông báo mẫu: loại, nguồn, loại tham chiếu, id tham chiếu (có thể null), số phút trước, đã đọc chưa. */
    record Item(String type, String source, String referenceType, UUID referenceId, String title, String content,
                int minutesAgo, boolean read) { }

    static List<Item> playerItems() {
        UUID futsalCup = tournament("Cúp Futsal Tết Dương Lịch 2027");
        return List.of(
                new Item("BOOKING", "VENUE", "BOOKING", null, "Đặt sân thành công",
                        "Sân 5 người 1 lúc 19:00 thứ Sáu đã được xác nhận. Đưa mã QR cho chủ sân khi tới nhé.", 95, false),
                new Item("PAYMENT", "PAYMENT", "PAYMENT", null, "Đã nhận tiền cọc",
                        "Thanh toán cọc 114.000 đ qua payOS thành công.", 100, true),
                new Item("TOURNAMENT", "CLUB", "TOURNAMENT", futsalCup, "Giải mới đang mở đăng ký",
                        "Cúp Futsal Tết Dương Lịch 2027 đã mở đăng ký, còn chỗ cho 8 đội. Lệ phí 700.000 đ/đội.", 180, false),
                new Item("MATCHMAKING", "AI", "MATCHMAKING_SESSION", null, "AI đã tìm được đối thủ phù hợp",
                        "Có một kèo phù hợp 87% với bạn tối thứ Bảy. Xem và chấp nhận trước khi hết hạn.", 1_440, false),
                new Item("FRIENDSHIP", "SOCIAL", "FRIENDSHIP", null, "Lời mời kết bạn mới",
                        "Một người từng ghép trận với bạn vừa gửi lời mời kết bạn.", 1_500, false),
                new Item("CLUB", "CLUB", "CLUB", null, "Buổi sinh hoạt CLB tối nay",
                        "CLB của bạn sinh hoạt lúc 19:00 hôm nay. Nhớ báo vắng nếu không đi được.", 300, true),
                new Item("COMMUNITY", "SOCIAL", "POST", null, "Bài viết của bạn có lượt thích mới",
                        "12 người đã thích bài viết của bạn trong cộng đồng.", 2_880, true),
                new Item("REFUND", "PAYMENT", "REFUND", null, "Khoản hoàn tiền đang chờ bạn",
                        "Khoản hoàn 57.000 đ do hủy sân đang chờ bạn thêm tài khoản ngân hàng để nhận.", 4_320, true),
                new Item("SYSTEM", "ADMIN", "SYSTEM", null, "GOAT Sports có gì mới",
                        "AI ghép kèo giờ hỗ trợ ghép theo đội và CLB với CLB. Thử ngay ở mục AI ghép trận!", 5_760, true));
    }

    static List<Item> ownerItems() {
        return List.of(
                new Item("BOOKING", "VENUE", "BOOKING", null, "Có đơn đặt sân mới",
                        "Khách vừa đặt Sân 7 người 1 lúc 19:00 tối mai và đã thanh toán cọc.", 30, false),
                new Item("CHECK_IN", "VENUE", "BOOKING", null, "Khách đã check-in",
                        "Khách đã quét mã nhận sân lúc 18:55, phần tiền còn lại đã thu tại quầy.", 150, true),
                new Item("REVIEW", "VENUE", "REVIEW", null, "Đánh giá 5 sao mới",
                        "\"Sân sạch, đèn sáng, chủ sân nhiệt tình.\" Trả lời để cảm ơn khách nhé.", 1_440, false),
                new Item("TOURNAMENT", "CLUB", "TOURNAMENT", null, "Có đội mới đăng ký giải",
                        "Một đội vừa đăng ký và thanh toán lệ phí giải của bạn.", 400, false),
                new Item("PAYMENT", "PAYMENT", "PAYMENT", null, "Rút tiền thành công",
                        "Tiền doanh thu tháng trước đã chuyển về tài khoản ngân hàng của bạn.", 20_160, true),
                new Item("REPORT_EXPORT", "REPORT", "REPORT_EXPORT", null, "Báo cáo định kỳ đã sẵn sàng",
                        "Báo cáo doanh thu tháng vừa rồi đã được tạo, có thể xuất PDF hoặc Excel.", 4_000, true));
    }

    static List<Item> moderatorItems() {
        return List.of(
                new Item("CONTENT_MODERATION", "SOCIAL", "CONTENT_REPORT", null, "Báo cáo vi phạm mới",
                        "Có 4 bài viết bị báo cáo đang chờ xử lý (spam, xúc phạm, lừa đảo).", 60, false),
                new Item("CONTENT_MODERATION", "SOCIAL", "CONTENT_APPEAL", null, "Kháng nghị nội dung mới",
                        "Một người dùng kháng nghị quyết định gỡ bài viết.", 600, false),
                new Item("SYSTEM", "AUTH", "SYSTEM", null, "Kháng nghị mở khóa tài khoản",
                        "Có 2 kháng nghị mở khóa tài khoản chưa xử lý.", 1_440, false),
                new Item("SYSTEM", "VENUE", "EXTERNAL_VENUE_SUGGESTION", null, "Đề xuất thêm sân mới",
                        "Người chơi vừa đề xuất 4 sân chưa có trên GOAT Sports.", 2_000, true));
    }

    @Override
    @Transactional
    public void run(String... args) {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        List<Object[]> rows = new ArrayList<>();
        PLAYERS.forEach(email -> addAll(rows, email, playerItems(), now));
        OWNERS.forEach(email -> addAll(rows, email, ownerItems(), now));
        addAll(rows, MODERATOR, moderatorItems(), now);
        int inserted = 0;
        for (int count : jdbc.batchUpdate("""
                insert into notifications (notification_id, event_id, receiver_id, title, content, type, status,
                                           source_service, reference_type, reference_id, read_at,
                                           created_at, created_by, updated_at, updated_by)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'demo-sim', ?, 'demo-sim') on conflict do nothing
                """, rows)) {
            inserted += Math.max(count, 0);
        }
        int broadcasts = broadcasts(now);
        log.info("Thông báo mẫu: {} thông báo, {} thông báo hàng loạt", inserted, broadcasts);
    }

    private void addAll(List<Object[]> rows, String email, List<Item> items, LocalDateTime now) {
        UUID receiverId = id("user", email);
        Integer existing = jdbc.queryForObject(
                "select count(*) from notifications where receiver_id = ? and created_by = 'demo-sim'", Integer.class,
                receiverId);
        if (existing != null && existing > 0) return;
        for (int index = 0; index < items.size(); index++) {
            Item item = items.get(index);
            LocalDateTime createdAt = now.minusMinutes(item.minutesAgo());
            UUID notificationId = id("demo-notification", email + "|" + index);
            rows.add(new Object[]{notificationId, notificationId, receiverId, item.title(), item.content(), item.type(),
                    item.read() ? "READ" : "UNREAD", item.source(), item.referenceType(), item.referenceId(),
                    item.read() ? Timestamp.valueOf(createdAt.plusMinutes(20)) : null, Timestamp.valueOf(createdAt),
                    Timestamp.valueOf(createdAt)});
        }
    }

    private int broadcasts(LocalDateTime now) {
        UUID sender = id("user", MODERATOR);
        Object[][] items = {
                {"Ra mắt ghép kèo theo đội", "AI ghép trận giờ hỗ trợ ghép đội từ bạn bè, đồng đội CLB và CLB với CLB.",
                        "ALL", 312, 20},
                {"Giải đấu cuối năm đang mở đăng ký", "Hơn 10 giải bóng đá, cầu lông, pickleball, bóng rổ, bóng chuyền và "
                        + "tennis đang mở đăng ký. Rủ đồng đội tham gia ngay!", "PLAYER", 297, 9},
                {"Cập nhật lịch đối soát doanh thu", "Từ tháng 12, doanh thu được giữ 3 ngày trước khi rút để kịp hoàn tiền "
                        + "khi khách hủy sân. Chi tiết trong mục Tài khoản nhận tiền.", "VENUE_OWNER", 21, 4}};
        List<Object[]> rows = new ArrayList<>();
        for (Object[] item : items) {
            LocalDateTime sentAt = now.minusDays((int) item[4]).withHour(9).withMinute(0);
            UUID broadcastId = id("demo-broadcast", (String) item[0]);
            rows.add(new Object[]{broadcastId, item[0], item[1], item[2], sender, "demo-broadcast-" + broadcastId,
                    item[3], Timestamp.valueOf(sentAt.plusMinutes(2)), Timestamp.valueOf(sentAt),
                    Timestamp.valueOf(sentAt.plusMinutes(2))});
        }
        int inserted = 0;
        for (int count : jdbc.batchUpdate("""
                insert into broadcasts (broadcast_id, title, content, audience, status, sent_by, idempotency_key,
                                        delivered_count, attempt_count, completed_at, created_at, created_by, updated_at,
                                        updated_by)
                values (?, ?, ?, ?, 'SENT', ?, ?, ?, 1, ?, ?, 'demo-sim', ?, 'demo-sim') on conflict do nothing
                """, rows)) {
            inserted += Math.max(count, 0);
        }
        return inserted;
    }

    static UUID tournament(String name) {
        return id("tournament", name);
    }

    static UUID id(String kind, String key) {
        return UUID.nameUUIDFromBytes(("goatsports:" + kind + ":" + key).getBytes(StandardCharsets.UTF_8));
    }
}
