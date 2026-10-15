package com.ceos24.mashup2backend.domain.remittance.repository;

import com.ceos24.mashup2backend.domain.remittance.entity.RecentRemittance;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RecentRemittanceRepository extends JpaRepository<RecentRemittance, Long> {

    // 유저의 가장 최근 송금 1건. sentAt이 동률이면 나중에 들어간 행(id 내림차순)을 쓴다.
    // 받는 통화를 함께 읽어 추가 조회를 막는다 (국가 코드·소수 자릿수·활성 여부가 모두 필요하다)
    @EntityGraph(attributePaths = "currency")
    Optional<RecentRemittance> findFirstByUserIdOrderBySentAtDescIdDesc(Long userId);
}
