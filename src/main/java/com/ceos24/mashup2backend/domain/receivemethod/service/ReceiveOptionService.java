package com.ceos24.mashup2backend.domain.receivemethod.service;

import com.ceos24.mashup2backend.domain.currency.entity.Currency;
import com.ceos24.mashup2backend.domain.currency.repository.CurrencyRepository;
import com.ceos24.mashup2backend.domain.exchangerate.entity.ExchangeRate;
import com.ceos24.mashup2backend.domain.exchangerate.repository.ExchangeRateRepository;
import com.ceos24.mashup2backend.domain.receivemethod.converter.ReceiveOptionConverter;
import com.ceos24.mashup2backend.domain.receivemethod.dto.enums.SortType;
import com.ceos24.mashup2backend.domain.receivemethod.dto.request.ReceiveOptionRequest;
import com.ceos24.mashup2backend.domain.receivemethod.dto.response.ReceiveOptionListResponse;
import com.ceos24.mashup2backend.domain.receivemethod.dto.response.ReceiveOptionResponse;
import com.ceos24.mashup2backend.domain.receivemethod.entity.ReceiveMethod;
import com.ceos24.mashup2backend.domain.receivemethod.exception.ReceiveOptionException;
import com.ceos24.mashup2backend.domain.receivemethod.exception.code.ReceiveOptionErrorCode;
import com.ceos24.mashup2backend.domain.receivemethod.repository.ReceiveMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReceiveOptionService {

    // 보내는 통화는 KRW만 지원한다
    private static final String SUPPORTED_SEND_CURRENCY = "KRW";

    private final ReceiveMethodRepository receiveMethodRepository;
    private final CurrencyRepository currencyRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    /***
     * 함수 기능: 보내려는 국가의 받는 방법 리스트를 조회한다.
     * @param request ReceiveOptionRequest (보내는 통화, 받는 통화, 금액, 정렬 기준)
     * @return ReceiveOptionListResponse (보내는 통화, 받는 통화, 금액 + 받는 방법 리스트)
     */
    public ReceiveOptionListResponse getReceiveOptions(ReceiveOptionRequest request) {

        // 현 정책 상 오직 KRW 통화만 받는다.
        if (!SUPPORTED_SEND_CURRENCY.equals(request.sendCurrency())) {
            throw new ReceiveOptionException(ReceiveOptionErrorCode.UNSUPPORTED_SEND_CURRENCY);
        }

        // 없는 통화와 비활성 통화를 같은 에러로 처리한다
        Currency receiveCurrency = currencyRepository.findByCurrencyCodeAndIsActiveTrue(request.receiveCurrency())
                .orElseThrow(() -> new ReceiveOptionException(ReceiveOptionErrorCode.RECEIVE_CURRENCY_NOT_FOUND));

        // 활성 통화인데 환율이 없으면 서버 데이터 정합성 문제다
        ExchangeRate exchangeRate = exchangeRateRepository.findById(receiveCurrency.getCurrencyCode())
                .orElseThrow(() -> new ReceiveOptionException(ReceiveOptionErrorCode.EXCHANGE_RATE_NOT_FOUND));

        // 통화의 노출 대상 받는 방법 전체를 조회한다. (선택 불가 옵션 포함)
        List<ReceiveMethod> methods = receiveMethodRepository.findActiveByReceiveCurrency(
                receiveCurrency.getCurrencyCode());

        // 선택 가능한 옵션만 수령액과 도착일을 계산해서 정렬한다.
        // 조회 결과가 id 오름차순이고 List.sort가 안정 정렬이므로 1차 키 동률은 id 순서가 유지된다
        List<CalculatedOption> selectable = methods.stream()
                .filter(method -> method.getUnselectableReason() == null)
                .map(method -> new CalculatedOption(
                        method,
                        method.calculateTotalAmount(request.amount(), exchangeRate.getBaseRate(),
                                receiveCurrency.getDecimalPlaces()), // 보내는 최종 금액 (수수료 제외)
                        method.calculateArrivalDate(LocalDate.now()))) // 도착 시간
                .sorted(comparatorOf(request.sort()))
                .toList();

        // 받는 방법 리스트 중 수령액 1위 선택. 동률이면 도착일이 빠른 쪽, 그래도 동률이면 id가 앞선 쪽
        CalculatedOption recommended = findRecommended(selectable);

        // 응답 DTO에 담을 '가능한(isActive=true && unselectableReason==null)' ReceiveOptionResponse 생성
        List<ReceiveOptionResponse> options = new ArrayList<>();
        for (CalculatedOption option : selectable) {
            options.add(ReceiveOptionConverter.toSelectableResponse(
                    option.method(), option.totalAmount(), option.arrivalDate(), option == recommended));
        }

        // 선택 불가 옵션은 정렬 기준과 무관하게 항상 맨 뒤에 붙인다
        methods.stream()
                .filter(method -> method.getUnselectableReason() != null)
                .forEach(method -> options.add(ReceiveOptionConverter.toUnselectableResponse(method)));

        return ReceiveOptionConverter.toListResponse(
                SUPPORTED_SEND_CURRENCY, // KRW
                receiveCurrency.getCurrencyCode(),
                request.amount(),
                options);
    }

    // 1차 정렬 키. 2차 키(id 오름차순)는 안정 정렬로 보장된다
    private Comparator<CalculatedOption> comparatorOf(SortType sort) {
        return switch (sort) {
            case TIME -> Comparator.comparing(CalculatedOption::arrivalDate);
            case FEE -> Comparator.comparing((CalculatedOption option) -> option.method().getFixedFee());
            case RATE -> Comparator.comparing(CalculatedOption::totalAmount, Comparator.reverseOrder());
        };
    }

    // 수령액 1위 하나. 동률이면 도착일이 빠른 쪽, 그래도 동률이면 id가 앞선 쪽
    private CalculatedOption findRecommended(List<CalculatedOption> options) {
        return options.stream()
                .max(Comparator.comparing(CalculatedOption::totalAmount)
                        .thenComparing(CalculatedOption::arrivalDate, Comparator.reverseOrder()))
                .orElse(null);
    }

    // 정렬과 뱃지 판정에 쓰는 계산 결과 묶음
    private record CalculatedOption(ReceiveMethod method, BigDecimal totalAmount, LocalDate arrivalDate) {
    }
}
