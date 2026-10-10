package com.ceos24.mashup2backend.domain.receivemethod.entity;

import com.ceos24.mashup2backend.domain.currency.entity.Currency;
import com.ceos24.mashup2backend.domain.receivemethod.entity.enums.ArrivalType;
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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "receive_method",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_receive_method",
                columnNames = {"receive_currency", "method_type", "method_provider", "speedType"}
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
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal fixedFee;

    // 비율 마진 (0.01 = 1%). 적용환율 = base_rate * (1 - rate_margin)
    @Column(nullable = false, precision = 6, scale = 5)
    private BigDecimal rateMargin;

    // 소요 시간 유형. arrival_value의 단위를 결정한다
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ArrivalType arrivalType;

    // 소요 값. IMMEDIATE면 분, BUSINESS_DAY면 영업일 수
    @Column(nullable = false)
    private Integer arrivalValue;

    // 모달에 띄우는 고정 문구 (ex. 송금 시간 15분 1일)
    @Column(nullable = false, length = 100)
    private String estimatedText;

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
                          ArrivalType arrivalType, Integer arrivalValue, String estimatedText,
                          BigDecimal minAmount, BigDecimal maxAmount, String unselectableReason,
                          Boolean isActive) {
        this.receiveCurrency = receiveCurrency;
        this.methodType = methodType;
        this.methodProvider = methodProvider;
        this.methodName = methodName;
        this.speedType = speedType;
        this.fixedFee = fixedFee;
        this.rateMargin = rateMargin;
        this.arrivalType = arrivalType;
        this.arrivalValue = arrivalValue;
        this.estimatedText = estimatedText;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.unselectableReason = unselectableReason;
        this.isActive = isActive;
    }
}