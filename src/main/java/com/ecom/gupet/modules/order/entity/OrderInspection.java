package com.ecom.gupet.modules.order.entity;

import com.ecom.gupet.modules.user.entity.User;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "order_inspections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderInspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    // Operator thực hiện kiểm tra
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspector_id", nullable = false)
    private User inspector;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InspectionResult result = InspectionResult.PENDING;

    // Danh sách hạng mục kiểm tra, cập nhật dần trong quá trình kiểm tra
    // Cấu trúc mẫu:
    // {
    //   "species_match":  {"label": "Đúng loài/giống",     "passed": true},
    //   "gender_match":   {"label": "Đúng giới tính",       "passed": true},
    //   "health_check":   {"label": "Tình trạng sức khỏe",  "passed": false, "note": "Có dấu hiệu mệt mỏi"},
    //   "weight_match":   {"label": "Cân nặng đúng mô tả",  "passed": true},
    //   "vaccine_docs":   {"label": "Có giấy tờ vaccine",    "passed": false, "note": "Thiếu sổ tiêm"}
    // }
    @Type(JsonType.class)
    @Column(columnDefinition = "json")
    private Map<String, Object> checklist;

    // Ghi chú tổng thể của operator
    @Column(columnDefinition = "TEXT")
    private String note;

    // Mảng URL ảnh/video bằng chứng kiểm tra
    @Type(JsonType.class)
    @Column(columnDefinition = "json")
    private List<String> evidenceImages;

    // Lý do từ chối (chỉ điền khi result = FAILED)
    private String rejectReason;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime startedAt;

    // NULL khi đang kiểm tra, điền khi có kết quả cuối cùng
    private LocalDateTime completedAt;

    public enum InspectionResult {
        PENDING,  // Đang kiểm tra
        PASSED,   // Đạt — tiếp nhận đơn
        FAILED    // Không đạt — từ chối đơn
    }
}