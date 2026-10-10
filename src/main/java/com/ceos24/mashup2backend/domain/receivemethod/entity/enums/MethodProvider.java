package com.ceos24.mashup2backend.domain.receivemethod.entity.enums;

// 받는 방법 상세 (제공사)
public enum MethodProvider {

    NONE,   // 은행, 페이나우, 계정 (제공사 없음)
    
    // 카드 하위
    VISA, ATM, 
    
    // 캐쉬픽업 하위
    MONEY_GRAM, VIET_COM_BANK, WING,
    
    //  모바일 월렛
    GOPAY, OVO, GENERAL
}