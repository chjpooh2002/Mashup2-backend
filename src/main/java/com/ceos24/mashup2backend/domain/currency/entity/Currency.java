package com.ceos24.mashup2backend.domain.currency.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "currency")
public class Currency {

    // 통화 코드 (ex. VND, SGD)
    @Id
    @Column(length = 3)
    private String currencyCode;

    // 국가 코드
    @Column(nullable = false, length = 2)
    private String countryCode;

    // 국가명
    @Column(nullable = false, length = 50)
    private String countryName;

    // 금액 소수 자릿수
    @Column(nullable = false)
    private Integer decimalPlaces;

    // 활성화 여부
    @Column(nullable = false)
    private Boolean isActive;

    @Builder
    private Currency(String currencyCode, String countryCode, String countryName,
                     Integer decimalPlaces, Boolean isActive) {
        this.currencyCode = currencyCode;
        this.countryCode = countryCode;
        this.countryName = countryName;
        this.decimalPlaces = decimalPlaces;
        this.isActive = isActive;
    }
}