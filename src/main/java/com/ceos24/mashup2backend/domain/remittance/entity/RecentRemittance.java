package com.ceos24.mashup2backend.domain.remittance.entity;

import com.ceos24.mashup2backend.domain.currency.entity.Currency;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "recent_remittance")
public class RecentRemittance { // 송금이력

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 보낸 통화
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "currency_code")
    private Currency currency;

    // 유저 ID. 현재는 1로 고정
    @Column(nullable = false)
    private Long userId;

    // 보냈던 원화 금액
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal sendAmount;

    // 보낸 시각
    @Column(nullable = false)
    private LocalDateTime sentAt;

    @Builder
    private RecentRemittance(Currency currency, Long userId, BigDecimal sendAmount, LocalDateTime sentAt) {
        this.currency = currency;
        this.userId = userId;
        this.sendAmount = sendAmount;
        this.sentAt = sentAt;
    }
}