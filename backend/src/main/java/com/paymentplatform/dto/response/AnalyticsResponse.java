package com.paymentplatform.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticsResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long totalPayments;
    private Long successfulPayments;
    private Long failedPayments;
    private Long pendingPayments;
    private Long retriedPayments;

    private BigDecimal totalVolume;
    private BigDecimal successVolume;

    private Double successRate;
    private Double averagePaymentAmount;

    private List<DailyStats> dailyStats;

    private Map<String, Long> paymentMethodBreakdown;
    private Map<String, Long> statusBreakdown;

    private List<RecentActivity> recentActivity;

    private ZonedDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStats implements Serializable {

        private static final long serialVersionUID = 1L;

        private String date;
        private Long total;
        private Long success;
        private Long failed;
        private BigDecimal volume;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivity implements Serializable {

        private static final long serialVersionUID = 1L;

        private String paymentReference;
        private BigDecimal amount;
        private String currency;
        private String status;
        private String paymentMethod;
        private ZonedDateTime createdAt;
    }
}