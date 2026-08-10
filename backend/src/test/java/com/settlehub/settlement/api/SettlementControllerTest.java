package com.settlehub.settlement.api;

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
class SettlementControllerTest {

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
    @DisplayName("배치 실행 후 정산 금액이 룰과 일치하고 확정할 수 있다")
    void batchConfirmReadyFlow() throws Exception {
        String agencyToken = login("agency@seoul.local", "Demo1234!");
        String adminToken = login("admin@cheongnim.local", "Demo1234!");

        String o1 = "SET-O1-" + System.nanoTime();
        String o2 = "SET-O2-" + System.nanoTime();

        // 다른 테스트 주문과 겹치지 않는 기간 사용
        createOrder(agencyToken, o1, 20_000, "2026-09-02T10:00:00");
        createOrder(agencyToken, o2, 10_001, "2026-09-03T11:00:00");

        MvcResult batchResult = mockMvc.perform(post("/api/v1/settlements/batch")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "periodStart":"2026-09-01T00:00:00",
                                  "periodEnd":"2026-09-08T00:00:00",
                                  "agencyId":%d
                                }
                                """.formatted(agencyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.processedOrderCount").value(2))
                .andExpect(jsonPath("$.data.createdSettlementCount").value(1))
                .andReturn();

        long batchJobId = objectMapper.readTree(batchResult.getResponse().getContentAsString())
                .path("data").path("batchJobId").asLong();

        mockMvc.perform(get("/api/v1/batch-jobs/" + batchJobId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));

        MvcResult listResult = mockMvc.perform(get("/api/v1/settlements")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("agencyId", String.valueOf(agencyId))
                        .param("merchantId", String.valueOf(merchantId))
                        .param("periodStart", "2026-09-01T00:00:00")
                        .param("periodEnd", "2026-09-08T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].totalMerchantSettlementAmount").value(19_501))
                .andExpect(jsonPath("$.data.content[0].totalPlatformFeeAmount").value(1_500))
                .andExpect(jsonPath("$.data.content[0].status").value("CALCULATED"))
                .andReturn();

        long settlementId = objectMapper.readTree(listResult.getResponse().getContentAsString())
                .path("data").path("content").get(0).path("id").asLong();

        mockMvc.perform(get("/api/v1/settlements/" + settlementId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lines.length()").value(2));

        mockMvc.perform(post("/api/v1/settlements/" + settlementId + "/confirm")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        mockMvc.perform(post("/api/v1/settlements/" + settlementId + "/ready-for-payout")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY_FOR_PAYOUT"));

        // 확정 이후 재배치 거부
        mockMvc.perform(post("/api/v1/settlements/batch")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "periodStart":"2026-09-01T00:00:00",
                                  "periodEnd":"2026-09-08T00:00:00",
                                  "agencyId":%d
                                }
                                """.formatted(agencyId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INVALID_STATE"));
    }

    @Test
    @DisplayName("MERCHANT는 본인 정산만 조회한다")
    void merchantScope() throws Exception {
        String agencyToken = login("agency@seoul.local", "Demo1234!");
        String adminToken = login("admin@cheongnim.local", "Demo1234!");
        String merchantToken = login("merchant@kimbap.local", "Demo1234!");

        String externalId = "SET-M-" + System.nanoTime();
        createOrder(agencyToken, externalId, 20_000, "2026-08-10T10:00:00");

        mockMvc.perform(post("/api/v1/settlements/batch")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "periodStart":"2026-08-10T00:00:00",
                                  "periodEnd":"2026-08-17T00:00:00",
                                  "agencyId":%d
                                }
                                """.formatted(agencyId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/settlements")
                        .header("Authorization", "Bearer " + merchantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].merchantId").value(merchantId));
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
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        String token = root.path("data").path("accessToken").asText();
        assertThat(token).isNotBlank();
        return token;
    }
}
