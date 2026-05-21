
// ============================================================
// FILE 5: DeliveryFailedRequest.java  — Operator báo giao thất bại
// ============================================================
package com.ecom.gupet.modules.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeliveryFailedRequest {

    @NotBlank(message = "Lý do giao thất bại không được để trống")
    private String reason;

    // true = thử giao lại, false = hủy đơn
    private boolean retry = false;

    // Ghi chú khi retry (ví dụ: "Hẹn giao lại ngày mai")
    private String retryNote;
}