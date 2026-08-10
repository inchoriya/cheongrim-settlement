package com.settlehub.order.api;

import com.settlehub.order.domain.OrderStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class OrderCsvParser {

    private static final DateTimeFormatter ORDERED_AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public List<ParsedCsvRow> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is required");
        }

        List<ParsedCsvRow> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String header = reader.readLine();
            if (header == null) {
                throw new IllegalArgumentException("CSV is empty");
            }
            validateHeader(header);

            String line;
            int rowNumber = 1;
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }
                rows.add(parseLine(rowNumber, line));
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to read CSV: " + ex.getMessage());
        }
        return rows;
    }

    private void validateHeader(String header) {
        String normalized = header.replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT);
        String expected = "externalorderid,merchantcode,orderamount,deliverytip,orderedat,status";
        if (!normalized.equals(expected)) {
            throw new IllegalArgumentException(
                    "Invalid CSV header. Expected: externalOrderId,merchantCode,orderAmount,deliveryTip,orderedAt,status"
            );
        }
    }

    private ParsedCsvRow parseLine(int rowNumber, String line) {
        String[] parts = line.split(",", -1);
        if (parts.length != 6) {
            return ParsedCsvRow.invalid(rowNumber, null, "INVALID_COLUMN_COUNT");
        }

        String externalOrderId = parts[0].trim();
        String merchantCode = parts[1].trim();
        String orderAmountRaw = parts[2].trim();
        String deliveryTipRaw = parts[3].trim();
        String orderedAtRaw = parts[4].trim();
        String statusRaw = parts[5].trim();

        if (externalOrderId.isBlank()) {
            return ParsedCsvRow.invalid(rowNumber, externalOrderId, "EXTERNAL_ORDER_ID_REQUIRED");
        }
        if (merchantCode.isBlank()) {
            return ParsedCsvRow.invalid(rowNumber, externalOrderId, "MERCHANT_CODE_REQUIRED");
        }

        long orderAmount;
        try {
            orderAmount = parseNonNegativeLong(orderAmountRaw, "ORDER_AMOUNT");
        } catch (IllegalArgumentException ex) {
            return ParsedCsvRow.invalid(rowNumber, externalOrderId, ex.getMessage());
        }

        long deliveryTip = 0L;
        if (!deliveryTipRaw.isBlank()) {
            try {
                deliveryTip = parseNonNegativeLong(deliveryTipRaw, "DELIVERY_TIP");
            } catch (IllegalArgumentException ex) {
                return ParsedCsvRow.invalid(rowNumber, externalOrderId, ex.getMessage());
            }
        }

        LocalDateTime orderedAt;
        try {
            orderedAt = LocalDateTime.parse(orderedAtRaw, ORDERED_AT_FORMAT);
        } catch (DateTimeParseException ex) {
            return ParsedCsvRow.invalid(rowNumber, externalOrderId, "INVALID_ORDERED_AT");
        }

        OrderStatus status = OrderStatus.CREATED;
        if (!statusRaw.isBlank()) {
            try {
                status = OrderStatus.valueOf(statusRaw.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                return ParsedCsvRow.invalid(rowNumber, externalOrderId, "INVALID_STATUS");
            }
        }

        return ParsedCsvRow.valid(
                rowNumber,
                externalOrderId,
                merchantCode,
                orderAmount,
                deliveryTip,
                orderedAt,
                status
        );
    }

    private long parseNonNegativeLong(String raw, String field) {
        if (raw.contains(".") || raw.contains("e") || raw.contains("E")) {
            throw new IllegalArgumentException(field + "_MUST_BE_INTEGER");
        }
        try {
            long value = Long.parseLong(raw);
            if (value < 0) {
                throw new IllegalArgumentException(field + "_MUST_BE_NON_NEGATIVE");
            }
            return value;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(field + "_INVALID");
        }
    }

    public record ParsedCsvRow(
            int rowNumber,
            boolean valid,
            String externalOrderId,
            String merchantCode,
            Long orderAmount,
            Long deliveryTip,
            LocalDateTime orderedAt,
            OrderStatus status,
            String errorReason
    ) {
        static ParsedCsvRow valid(
                int rowNumber,
                String externalOrderId,
                String merchantCode,
                long orderAmount,
                long deliveryTip,
                LocalDateTime orderedAt,
                OrderStatus status
        ) {
            return new ParsedCsvRow(
                    rowNumber, true, externalOrderId, merchantCode,
                    orderAmount, deliveryTip, orderedAt, status, null
            );
        }

        static ParsedCsvRow invalid(int rowNumber, String externalOrderId, String reason) {
            return new ParsedCsvRow(
                    rowNumber, false, externalOrderId, null,
                    null, null, null, null, reason
            );
        }
    }
}
