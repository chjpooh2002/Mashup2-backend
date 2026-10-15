package com.ceos24.mashup2backend.domain.remittance.dto.response;

public record LatestRemittanceResponse(

        // 쓸 수 있는 송금 이력이 있었는지 여부. false면 아래 값은 기본값(KRW 10,000 -> VND)이다
        boolean hasHistory,

        // 보내는 쪽. 항상 KR / KRW
        RemittanceAmountResponse send,

        // 받는 쪽. 이력의 받는 통화로 환산한 금액
        RemittanceAmountResponse receive,

        // 현재 환율
        RemittanceExchangeRateResponse exchangeRate

) {
}
