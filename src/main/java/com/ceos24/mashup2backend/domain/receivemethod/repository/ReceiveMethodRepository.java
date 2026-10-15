package com.ceos24.mashup2backend.domain.receivemethod.repository;

import com.ceos24.mashup2backend.domain.receivemethod.entity.ReceiveMethod;
import com.ceos24.mashup2backend.domain.receivemethod.entity.enums.MethodProvider;
import com.ceos24.mashup2backend.domain.receivemethod.entity.enums.MethodType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReceiveMethodRepository extends JpaRepository<ReceiveMethod, Long> {

    // 특정 받는 통화의 활성 수취 방법 목록.
    // 2차 키인 id 오름차순으로만 순서를 고정한다.
    @Query("""
            select rm
            from ReceiveMethod rm
            join fetch rm.receiveCurrency c
            where c.currencyCode = :receiveCurrency
              and c.isActive = true
              and rm.isActive = true
            order by rm.id asc
            """)
    List<ReceiveMethod> findActiveByReceiveCurrency(@Param("receiveCurrency") String receiveCurrency);

    // 받는 방법 하나의 속도별 행 목록.
    @Query("""
            select rm
            from ReceiveMethod rm
            join rm.receiveCurrency c
            where c.currencyCode = :receiveCurrency
              and rm.methodType = :methodType
              and rm.methodProvider = :methodProvider
              and c.isActive = true
              and rm.isActive = true
            order by rm.id asc
            """)
    List<ReceiveMethod> findActiveByReceiveCurrencyAndMethod(@Param("receiveCurrency") String receiveCurrency,
                                                             @Param("methodType") MethodType methodType,
                                                             @Param("methodProvider") MethodProvider methodProvider);
}
