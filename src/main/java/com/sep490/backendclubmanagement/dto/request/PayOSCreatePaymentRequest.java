package com.sep490.backendclubmanagement.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class PayOSCreatePaymentRequest {
    private Long orderCode;
    private Long amount;
    private String description;
    private String buyerName;
    private String buyerCompanyName;
    private String buyerTaxCode;
    private String buyerAddress;
    private String buyerEmail;
    private String buyerPhone;
    private List<Item> items;
    private String cancelUrl;
    private String returnUrl;
    private Invoice invoice;
    private Long expiredAt;
    private String signature;

    @Data
    public static class Item {
        private String name;
        private Integer quantity;
        private Long price;
        private String unit;
        private Integer taxPercentage;
    }

    @Data
    public static class Invoice {
        private Boolean buyerNotGetInvoice;
        private Integer taxPercentage;
    }
}
