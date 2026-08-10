package com.settlehub.payout.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.settlehub.organization.domain.Agency;
import com.settlehub.organization.domain.AgencyRepository;
import com.settlehub.organization.domain.Merchant;
import com.settlehub.organization.domain.MerchantRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PayoutControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
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

    @Test
    @DisplayName("Mock 지급 성공 시 settlement가 PAID가 된다")
    void payoutSuccess() throws Exception {
        String adminToken = login("admin@cheongnim.local", "Demo1234!");
        long settlementId = prepareReadySettlement(adminToken, "2026-10-01T00:00:00", "2026-10-08T00:00:00",
                "2026-10-02T10:00:00", 20_000);

        mockMvc.perform(post("/api/v1/payouts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"settlementId":%d,"forceFail":false}
                                """.formatted(settlementId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data.amount").value(14_000))
                .andExpect(jsonPath("$.data.pgProvider").value("MOCK"))
                .andExpect(jsonPath("$.data.pgTransactionId").isNotEmpty());

        mockMvc.perform(get("/api/v1/settlements/" + settlementId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));

        mockMvc.perform(get("/api/v1/payouts")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("settlementId", String.valueOf(settlementId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("SUCCEEDED"));
    }

    @Test
    @DisplayName("forceFail 시 PAYOUT_FAILED가 되고 재시도할 수 있다")
    void payoutFailThenRetry() throws Exception {
        String adminToken = login("admin@cheongnim.local", "Demo1234!");
        long settlementId = prepareReadySettlement(adminToken, "2026-11-01T00:00:00", "2026-11-08T00:00:00",
                "2026-11-02T10:00:00", 20_000);

        mockMvc.perform(post("/api/v1/payouts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"settlementId":%d,"forceFail":true}
                                """.formatted(settlementId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("FAILED"))
                .andExpect(jsonPath("$.data.failureReason").value("FORCE_FAIL"));

        mockMvc.perform(get("/api/v1/settlements/" + settlementId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAYOUT_FAILED"));

        mockMvc.perform(post("/api/v1/payouts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"settlementId":%d,"forceFail":false}
                                """.formatted(settlementId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCEEDED"));

        mockMvc.perform(get("/api/v1/settlements/" + settlementId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));
    }

    private long prepareReadySettlement(
            String adminToken,
            String periodStart,
            String periodEnd,
            String orderedAt,
            long amount
    ) throws Exception {
        String agencyToken = login("agency@seoul.local", "Demo1234!");
        String externalId = "PAY-" + System.nanoTime();

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + agencyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalOrderId":"%s",
                                  "merchantId":%d,
                                  "orderAmount":%d,
                                  "orderedAt":"%s"
                                }
                                """.formatted(externalId, merchantId, amount, orderedAt)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/settlements/batch")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "periodStart":"%s",
                                  "periodEnd":"%s",
                                  "agencyId":%d
                                }
                                """.formatted(periodStart, periodEnd, agencyId)))
                .andExpect(status().isOk());

        MvcResult list = mockMvc.perform(get("/api/v1/settlements")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("agencyId", String.valueOf(agencyId))
                        .param("merchantId", String.valueOf(merchantId))
                        .param("periodStart", periodStart)
                        .param("periodEnd", periodEnd))
                .andExpect(status().isOk())
                .andReturn();

        long settlementId = objectMapper.readTree(list.getResponse().getContentAsString())
                .path("data").path("content").get(0).path("id").asLong();

        mockMvc.perform(post("/api/v1/settlements/" + settlementId + "/confirm")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/settlements/" + settlementId + "/ready-for-payout")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        return settlementId;
    }

    private String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = root.path("data").path("accessToken").asText();
        assertThat(token).isNotBlank();
        return token;
    }
}
