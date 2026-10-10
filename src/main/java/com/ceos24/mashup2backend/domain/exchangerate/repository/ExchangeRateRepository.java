package com.ceos24.mashup2backend.domain.exchangerate.repository;

import com.ceos24.mashup2backend.domain.exchangerate.entity.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;

// PK가 통화 코드이므로 findById(currencyCode)로 조회한다 (@MapsId로 currency와 PK 공유)
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, String> {

}
