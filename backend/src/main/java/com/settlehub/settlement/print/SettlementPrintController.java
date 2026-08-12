package com.settlehub.settlement.print;

import com.settlehub.auth.security.AuthUser;
import com.settlehub.common.api.PageResponse;
import com.settlehub.common.exception.BusinessException;
import com.settlehub.settlement.api.SettlementResponse;
import com.settlehub.settlement.api.SettlementService;
import com.settlehub.settlement.domain.SettlementStatus;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;

/**
 * 운영자용 정산서 출력 화면.
 *
 * <p>정산서는 인쇄·PDF 보관이 목적이라 클라이언트 렌더링이 이점이 없다.
 * 조회 권한 판정은 REST API와 동일한 {@link SettlementService}를 그대로 재사용해,
 * 화면이 달라도 접근 범위 규칙이 갈라지지 않도록 한다.
 */
@Controller
@RequestMapping("/print")
@RequiredArgsConstructor
public class SettlementPrintController {

    private static final int PAGE_SIZE = 20;

    private final SettlementService settlementService;

    @GetMapping("/login")
    public String loginForm() {
        return "print/login";
    }

    @GetMapping("/settlements")
    public String list(
            @AuthenticationPrincipal AuthUser actor,
            @RequestParam(required = false) SettlementStatus status,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {
        PageResponse<SettlementResponse> result = settlementService.list(
                actor,
                status,
                null,
                null,
                null,
                null,
                PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "periodStart"))
        );

        model.addAttribute("result", result);
        model.addAttribute("actor", actor);
        model.addAttribute("status", status);
        model.addAttribute("statuses", SettlementStatus.values());
        return "print/list";
    }

    @GetMapping("/settlements/{id}")
    public String detail(
            @AuthenticationPrincipal AuthUser actor,
            @PathVariable Long id,
            Model model
    ) {
        SettlementResponse settlement = settlementService.get(actor, id);

        // 문서로 고정한 검산식: 가맹점 + 플랫폼 + 대행사 + 라이더(팁 포함) = 주문금액
        long distributed = settlement.totalMerchantSettlementAmount()
                + settlement.totalPlatformFeeAmount()
                + settlement.totalAgencySettlementAmount()
                + settlement.totalRiderFeeAmount()
                + settlement.totalTipAmount();

        model.addAttribute("s", settlement);
        model.addAttribute("actor", actor);
        model.addAttribute("distributed", distributed);
        model.addAttribute("balanced", distributed == settlement.totalOrderAmount());
        model.addAttribute("issuedAt", LocalDateTime.now());
        return "print/settlement";
    }

    /**
     * {@code GlobalExceptionHandler}는 {@code @RestControllerAdvice}라 JSON을 돌려준다.
     * 화면 요청에는 화면으로 응답해야 하므로 이 컨트롤러 안에서 먼저 가로챈다.
     */
    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException ex, Model model, HttpServletResponse response) {
        response.setStatus(ex.getStatus().value());
        model.addAttribute("status", ex.getStatus().value());
        model.addAttribute("message", ex.getMessage());
        return "print/error";
    }
}
