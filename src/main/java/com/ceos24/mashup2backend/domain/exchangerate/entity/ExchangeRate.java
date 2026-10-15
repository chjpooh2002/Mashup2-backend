package com.ceos24.mashup2backend.domain.exchangerate.entity;

import com.ceos24.mashup2backend.domain.currency.entity.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "exchange_rate")
public class ExchangeRate {

    // 통화 코드. currency와 PK를 공유한다 (@MapsId)
    @Id
    private String currencyCode;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_code")
    private Currency currency;

    // 매매기준율. 1원당 받는 통화 값
    @Column(nullable = false, precision = 20, scale = 10)
    private BigDecimal baseRate;

    // 고시 시각
    @Column(nullable = false)
    private LocalDateTime baseTime;

    // 갱신 시각
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // currencyCode는 @MapsId가 currency로부터 채우므로 빌더에서 받지 않는다
    @Builder
    private ExchangeRate(Currency currency, BigDecimal baseRate,
                         LocalDateTime baseTime, LocalDateTime updatedAt) {
        this.currency = currency;
        this.baseRate = baseRate;
        this.baseTime = baseTime;
        this.updatedAt = updatedAt;
    }

    // 새로 받은 환율로 갱신한다
    public void refresh(BigDecimal baseRate, LocalDateTime baseTime, LocalDateTime updatedAt) {
        this.baseRate = baseRate;
        this.baseTime = baseTime;
        this.updatedAt = updatedAt;
    }
}