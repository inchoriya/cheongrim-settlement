package com.settlehub.organization.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("ADMIN은 대행사/가맹점을 생성하고 조회할 수 있다")
    void adminCreatesAgencyAndMerchant() throws Exception {
        String token = login("admin@cheongrim.local", "Demo1234!");
        String code = "AG-NEW-" + System.nanoTime();

        MvcResult agencyResult = mockMvc.perform(post("/api/v1/agencies")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"code":"%s","name":"신규대행"}
                                """.formatted(code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value(code))
                .andReturn();

        long agencyId = objectMapper.readTree(agencyResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/merchants")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"agencyId":%d,"code":"M-NEW","name":"신규가맹"}
                                """.formatted(agencyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("M-NEW"));

        mockMvc.perform(get("/api/v1/agencies")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("AGENCY는 감사 로그에 접근할 수 없다")
    void agencyCannotReadAuditLogs() throws Exception {
        String token = login("agency@seoul.local", "Demo1234!");
        mockMvc.perform(get("/api/v1/audit-logs")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
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
