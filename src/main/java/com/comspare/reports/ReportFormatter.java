package com.comspare.reports;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Turns raw database rows into clean display rows (used by both the web page and the PDF). */
final class ReportFormatter {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private ReportFormatter() {
    }

    static List<Map<String, Object>> format(List<Map<String, Object>> rows, boolean prettyHeaders) {
        DecimalFormat money = new DecimalFormat("#,##0.00");
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            Map<String, Object> clean = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : row.entrySet()) {
                String key = prettyHeaders ? pretty(e.getKey()) : e.getKey();
                clean.put(key, value(e.getValue(), money));
            }
            result.add(clean);
        }
        return result;
    }

    private static Object value(Object v, DecimalFormat money) {
        if (v == null) return "";
        if (v instanceof Timestamp t) return t.toLocalDateTime().format(DATE_TIME);
        if (v instanceof LocalDateTime t) return t.format(DATE_TIME);
        if (v instanceof BigDecimal d) return money.format(d.setScale(2, RoundingMode.HALF_UP));
        if (v instanceof Double || v instanceof Float) return money.format(((Number) v).doubleValue());
        if (v instanceof Boolean b) return b ? "Yes" : "No";
        return v; // integers, strings, dates stay as they are
    }

    /** ProductCode -> Product Code, MovementID -> Movement ID */
    static String pretty(String column) {
        return column == null ? "" : column.replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ");
    }
}
