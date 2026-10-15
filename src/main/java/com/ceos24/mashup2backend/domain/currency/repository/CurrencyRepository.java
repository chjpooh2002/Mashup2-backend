package com.ceos24.mashup2backend.domain.currency.repository;

import com.ceos24.mashup2backend.domain.currency.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CurrencyRepository extends JpaRepository<Currency, String> {

    Optional<Currency> findByCurrencyCodeAndIsActiveTrue(String currencyCode);

    List<Currency> findAllByIsActiveTrue();
}
