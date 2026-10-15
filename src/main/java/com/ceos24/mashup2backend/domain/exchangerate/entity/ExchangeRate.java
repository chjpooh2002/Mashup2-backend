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
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "exchange_rate")
public class ExchangeRate {

    // 화면 표시용 환율 소수 자릿수. 어느 방향으로 표시하든 2자리로 맞춘다
    private static final int DISPLAY_SCALE = 2;

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

    // 원화 송금액을 받는 통화로 환산한다. 수수료와 환율 마진은 반영하지 않는다
    public BigDecimal convertFromKrw(BigDecimal krwAmount, int decimalPlaces) {
        return krwAmount
                .multiply(baseRate)
                .setScale(decimalPlaces, RoundingMode.HALF_UP);
    }

    // 원화 기준(1원 = N)으로 표시할지 여부.
    // 1원당 값이 1보다 작으면 0.00094처럼 읽히지 않아 방향을 뒤집는다 (ex. SGD)
    public boolean isKrwBasedDisplay() {
        return baseRate.compareTo(BigDecimal.ONE) >= 0;
    }

    // 표시용 환율. 원화 기준이면 baseRate, 방향을 뒤집으면 1 / baseRate.
    // 환산에는 쓰지 않는다 — 반올림한 값으로 역산하면 금액이 틀어진다
    public BigDecimal displayRate() {
        if (isKrwBasedDisplay()) {
            return baseRate.setScale(DISPLAY_SCALE, RoundingMode.HALF_UP);
        }
        // scale을 지정하지 않으면 무한소수에서 ArithmeticException이 난다
        return BigDecimal.ONE.divide(baseRate, DISPLAY_SCALE, RoundingMode.HALF_UP);
    }
}