
// ============================================================
// FILE 4: InspectionRequest.java  — Operator gửi kết quả kiểm tra
// ============================================================
package com.ecom.gupet.modules.order.dto;

import com.ecom.gupet.modules.order.entity.OrderInspection.InspectionResult;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class InspectionRequest {

    @NotNull
    private InspectionResult result; // PASSED hoặc FAILED

    // Checklist các hạng mục kiểm tra
    private Map<String, Object> checklist;

    private String note;

    // URL ảnh bằng chứng (đã upload trước)
    private List<String> evidenceImages;

    // Bắt buộc điền nếu result = FAILED
    private String rejectReason;
}
