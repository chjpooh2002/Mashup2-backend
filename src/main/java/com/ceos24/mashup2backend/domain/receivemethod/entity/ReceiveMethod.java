package com.ceos24.mashup2backend.domain.receivemethod.entity;

import com.ceos24.mashup2backend.domain.currency.entity.Currency;
import com.ceos24.mashup2backend.domain.receivemethod.entity.enums.MethodProvider;
import com.ceos24.mashup2backend.domain.receivemethod.entity.enums.MethodType;
import com.ceos24.mashup2backend.domain.receivemethod.entity.enums.SpeedType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "receive_method",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_receive_method",
                columnNames = {"receive_currency", "method_type", "method_provider", "speed_type"}
        )
)
public class ReceiveMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 받는 통화 (ex. VND, SGD)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receive_currency")
    private Currency receiveCurrency;

    // 받는 방법
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MethodType methodType;

    // 받는 방법 상세
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MethodProvider methodProvider;

    // 카드에 표시되는 이름
    @Column(nullable = false, length = 100)
    private String methodName;

    // 송금 속도
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SpeedType speedType;

    // 고정 수수료. 보내는 통화(KRW) 기준
    @Column(nullable = false, precision = 19)
    private BigDecimal fixedFee;

    // 비율 마진 (0.01 = 1%). 적용환율 = base_rate * (1 - rate_margin)
    @Column(nullable = false, precision = 6, scale = 3)
    private BigDecimal rateMargin;

    // 최소 금액
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal minAmount;

    // 최대 금액. 한도가 없으면 null
    @Column(precision = 19, scale = 4)
    private BigDecimal maxAmount;

    // 선택 불가 사유 (ex. *지원하지 않습니다). 선택 가능하면 null
    @Column(length = 100)
    private String unselectableReason;

    // 활성화 여부
    @Column(nullable = false)
    private Boolean isActive;

    @Builder
    private ReceiveMethod(Currency receiveCurrency, MethodType methodType, MethodProvider methodProvider,
                          String methodName, SpeedType speedType, BigDecimal fixedFee, BigDecimal rateMargin,
                          BigDecimal minAmount, BigDecimal maxAmount, String unselectableReason,
                          Boolean isActive) {
        this.receiveCurrency = receiveCurrency;
        this.methodType = methodType;
        this.methodProvider = methodProvider;
        this.methodName = methodName;
        this.speedType = speedType;
        this.fixedFee = fixedFee;
        this.rateMargin = rateMargin;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.unselectableReason = unselectableReason;
        this.isActive = isActive;
    }

    // 최종 보내는 금액 (수수료 제외) = 송금 금액(KRW) * 기준 환율 * (1 - 환율 마진)
    public BigDecimal calculateTotalAmount(BigDecimal sendAmount, BigDecimal baseRate, int decimalPlaces) {
        return sendAmount
                .multiply(baseRate)
                .multiply(BigDecimal.ONE.subtract(rateMargin))
                .setScale(decimalPlaces, RoundingMode.CEILING);
    }

    // 도착 예정일. speedType의 가산 일수를 더한다
    public LocalDate calculateArrivalDate(LocalDate now) {
        return now.plusDays(speedType.getArrivalDays());
    }
}