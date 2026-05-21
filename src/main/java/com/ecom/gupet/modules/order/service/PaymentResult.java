package com.ecom.gupet.modules.order.service;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentResult {
    private boolean success;
    private String  gatewayRef;  // mã giao dịch — mock sinh UUID, thật lấy từ cổng TT
    private String  message;
}
