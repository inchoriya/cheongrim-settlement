package com.settlehub.order.api;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderCsvParserTest {

    private final OrderCsvParser parser = new OrderCsvParser();

    @Test
    void parsesValidAndInvalidRows() {
        String csv = """
                externalOrderId,merchantCode,orderAmount,deliveryTip,orderedAt,status
                ORD-1,M-001,20000,0,2026-08-01T12:30:00,CREATED
                ORD-2,M-001,100.5,0,2026-08-01T12:30:00,CREATED
                ORD-3,M-001,10001,,2026-08-01T13:00:00,
                """;

        List<OrderCsvParser.ParsedCsvRow> rows = parser.parse(multipart(csv));

        assertThat(rows).hasSize(3);
        assertThat(rows.get(0).valid()).isTrue();
        assertThat(rows.get(0).orderAmount()).isEqualTo(20_000);
        assertThat(rows.get(1).valid()).isFalse();
        assertThat(rows.get(1).errorReason()).isEqualTo("ORDER_AMOUNT_MUST_BE_INTEGER");
        assertThat(rows.get(2).valid()).isTrue();
        assertThat(rows.get(2).deliveryTip()).isEqualTo(0);
        assertThat(rows.get(2).status().name()).isEqualTo("CREATED");
    }

    @Test
    void rejectsBadHeader() {
        assertThatThrownBy(() -> parser.parse(multipart("a,b,c\n1,2,3")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid CSV header");
    }

    private MockMultipartFile multipart(String content) {
        return new MockMultipartFile(
                "file",
                "orders.csv",
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }
}
