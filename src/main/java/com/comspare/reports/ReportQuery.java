package com.comspare.reports;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Builds a SQL string and its parameters together, so they can never get out of order. */
final class ReportQuery {

    private final StringBuilder sql;
    private final List<Object> args = new ArrayList<>();

    ReportQuery(String baseSql) {
        this.sql = new StringBuilder(baseSql);
    }

    /** Adds "AND condition" (with one ? parameter) only when the value was supplied. */
    ReportQuery and(String condition, String value) {
        if (value != null && !value.isBlank()) {
            sql.append(" AND ").append(condition).append(' ');
            args.add(value.trim());
        }
        return this;
    }

    /** Adds fixed SQL (no parameters). */
    ReportQuery raw(String fragment) {
        sql.append(' ').append(fragment).append(' ');
        return this;
    }

    List<Map<String, Object>> run(JdbcTemplate jdbc) {
        return jdbc.queryForList(sql.toString(), args.toArray());
    }
}
