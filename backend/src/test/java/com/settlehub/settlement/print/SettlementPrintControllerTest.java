package com.settlehub.settlement.print;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.settlehub.auth.security.AuthUser;
import com.settlehub.organization.domain.Agency;
import com.settlehub.organization.domain.AgencyRepository;
import com.settlehub.organization.domain.Merchant;
import com.settlehub.organization.domain.MerchantRepository;
import com.settlehub.organization.domain.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class SettlementPrintControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private AgencyRepository agencyRepository;
    @Autowired
    private MerchantRepository merchantRepository;

    private Long agencyId;
    private Long merchantId;

    @BeforeEach
    void setUp() {
        Agency agency = agencyRepository.findByCode("AG-SEOUL-01").orElseThrow();
        Merchant merchant = merchantRepository.findByAgencyIdAndCode(agency.getId(), "M-001").orElseThrow();
        agencyId = agency.getId();
        merchantId = merchant.getId();
    }

    private AuthUser actor(String email) {
        return AuthUser.from(userAccountRepository.findByEmail(email).orElseThrow());
    }

    @Test
    @DisplayName("인쇄 화면은 미인증이면 로그인 페이지로 보낸다")
    void redirectsToLoginWhenAnonymous() throws Exception {
        mockMvc.perform(get("/print/settlements"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/print/login"));
    }

    @Test
    @DisplayName("API 체인은 세션 로그인으로 넘어가지 않고 그대로 401을 준다")
    void apiChainStaysStateless() throws Exception {
        mockMvc.perform(get("/api/v1/settlements"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("폼 로그인에 성공하면 정산 목록으로 이동한다")
    void formLoginSucceeds() throws Exception {
        mockMvc.perform(formLogin("/print/login")
                        .user("admin@cheongnim.local")
                        .password("Demo1234!"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/print/settlements"));
    }

    @Test
    @DisplayName("잘못된 비밀번호는 로그인 실패로 처리한다")
    void formLoginFails() throws Exception {
        mockMvc.perform(formLogin("/print/login")
                        .user("admin@cheongnim.local")
                        .password("wrong-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/print/login?error"));
    }

    @Test
    @DisplayName("최초 실행 직후에도 시드 정산이 있어 목록이 비어 있지 않다")
    void rendersListForAuthenticatedUser() throws Exception {
        String html = mockMvc.perform(get("/print/settlements").with(user(actor("admin@cheongnim.local"))))
                .andExpect(status().isOk())
                .andExpect(view().name("print/list"))
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(html).doesNotContain("조회된 정산 건이 없습니다");
        assertThat(html).contains("김밥천국");
    }

    @Test
    @DisplayName("없는 정산 건은 JSON이 아니라 화면으로 응답한다")
    void rendersErrorPageInsteadOfJson() throws Exception {
        mockMvc.perform(get("/print/settlements/{id}", 999_999_999L)
                        .with(user(actor("admin@cheongnim.local"))))
                .andExpect(status().isNotFound())
                .andExpect(view().name("print/error"))
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    @DisplayName("정산서를 렌더링하면 명세와 검산 결과가 화면에 찍힌다")
    void rendersSettlementSheet() throws Exception {
        String agencyToken = login("agency@seoul.local", "Demo1234!");
        String adminToken = login("admin@cheongnim.local", "Demo1234!");

        // 다른 테스트와 겹치지 않는 기간
        createOrder(agencyToken, "PRINT-O1-" + System.nanoTime(), 20_000, "2026-10-06T10:00:00");
        createOrder(agencyToken, "PRINT-O2-" + System.nanoTime(), 13_500, "2026-10-07T12:00:00");

        mockMvc.perform(post("/api/v1/settlements/batch")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "periodStart":"2026-10-05T00:00:00",
                                  "periodEnd":"2026-10-12T00:00:00",
                                  "agencyId":%d
                                }
                                """.formatted(agencyId)))
                .andExpect(status().isOk());

        MvcResult listResult = mockMvc.perform(get("/api/v1/settlements")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("periodStart", "2026-10-05T00:00:00")
                        .param("periodEnd", "2026-10-12T00:00:00"))
                .andExpect(status().isOk())
                .andReturn();

        long settlementId = objectMapper.readTree(listResult.getResponse().getContentAsString())
                .path("data").path("content").get(0).path("id").asLong();

        String html = mockMvc.perform(get("/print/settlements/{id}", settlementId)
                        .with(user(actor("admin@cheongnim.local"))))
                .andExpect(status().isOk())
                .andExpect(view().name("print/settlement"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // 서버가 실제로 값을 채워 내려보냈는지 — 빈 껍데기가 아닌지 확인한다
        assertThat(html).contains("정 산 서");
        assertThat(html).contains("주문별 명세");
        assertThat(html).contains("2026-10-05");           // #temporals 포맷
        assertThat(html).contains("20,000");               // #numbers 천단위 구분
        assertThat(html).contains("(일치)");               // 검산식 통과
        assertThat(html).doesNotContain("(불일치");
    }

    private void createOrder(String token, String externalId, long amount, String orderedAt) throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalOrderId":"%s",
                                  "merchantId":%d,
                                  "orderAmount":%d,
                                  "deliveryTip":0,
                                  "orderedAt":"%s"
                                }
                                """.formatted(externalId, merchantId, amount, orderedAt)))
                .andExpect(status().isOk());
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }
}
