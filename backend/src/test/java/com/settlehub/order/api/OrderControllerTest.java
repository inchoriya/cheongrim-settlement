package com.settlehub.order.api;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerTest {

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
    @DisplayName("AGENCY는 주문을 생성하고 조회할 수 있다")
    void agencyCreatesAndListsOrder() throws Exception {
        String token = login("agency@seoul.local", "Demo1234!");
        String externalId = "ORD-API-" + System.nanoTime();

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalOrderId":"%s",
                                  "agencyId":%d,
                                  "merchantId":%d,
                                  "orderAmount":20000,
                                  "deliveryTip":0,
                                  "orderedAt":"2026-08-01T12:30:00"
                                }
                                """.formatted(externalId, agencyId, merchantId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.externalOrderId").value(externalId))
                .andExpect(jsonPath("$.data.orderAmount").value(20000));

        mockMvc.perform(get("/api/v1/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("CSV 업로드는 성공/실패를 행 단위로 반환한다")
    void csvUploadPartialSuccess() throws Exception {
        String token = login("agency@seoul.local", "Demo1234!");
        String okId = "ORD-CSV-OK-" + System.nanoTime();
        String dupId = "ORD-CSV-DUP-" + System.nanoTime();

        mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalOrderId":"%s",
                                  "merchantId":%d,
                                  "orderAmount":20000,
                                  "orderedAt":"2026-08-01T10:00:00"
                                }
                                """.formatted(dupId, merchantId)))
                .andExpect(status().isOk());

        String csv = """
                externalOrderId,merchantCode,orderAmount,deliveryTip,orderedAt,status
                %s,M-001,20000,0,2026-08-01T12:30:00,CREATED
                %s,M-001,15000,1000,2026-08-01T13:00:00,CREATED
                BAD,M-001,12.3,0,2026-08-01T14:00:00,CREATED
                """.formatted(okId, dupId);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "orders.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/orders/upload")
                        .file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRows").value(3))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failureCount").value(2));
    }

    @Test
    @DisplayName("MERCHANT는 타 가맹점 주문을 조회할 수 없다")
    void merchantCannotReadOtherMerchantOrder() throws Exception {
        String agencyToken = login("agency@seoul.local", "Demo1234!");
        Merchant other = merchantRepository.findByAgencyIdAndCode(agencyId, "M-002").orElseThrow();
        String externalId = "ORD-OTHER-" + System.nanoTime();

        MvcResult created = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + agencyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalOrderId":"%s",
                                  "merchantId":%d,
                                  "orderAmount":10000,
                                  "orderedAt":"2026-08-01T15:00:00"
                                }
                                """.formatted(externalId, other.getId())))
                .andExpect(status().isOk())
                .andReturn();

        long orderId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        String merchantToken = login("merchant@kimbap.local", "Demo1234!");
        mockMvc.perform(get("/api/v1/orders/" + orderId)
                        .header("Authorization", "Bearer " + merchantToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("주문 취소 후 상태가 CANCELLED가 된다")
    void cancelOrder() throws Exception {
        String token = login("agency@seoul.local", "Demo1234!");
        String externalId = "ORD-CANCEL-" + System.nanoTime();

        MvcResult created = mockMvc.perform(post("/api/v1/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "externalOrderId":"%s",
                                  "merchantId":%d,
                                  "orderAmount":9000,
                                  "orderedAt":"2026-08-01T16:00:00"
                                }
                                """.formatted(externalId, merchantId)))
                .andExpect(status().isOk())
                .andReturn();

        long orderId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/cancel")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
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
