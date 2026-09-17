package com.comspare.reports;

import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PdfExportService {

    private final DecimalFormat decimalFormat =
            new DecimalFormat("#,##0.00");

    public void export(
            String title,
            List<Map<String, Object>> rows,
            OutputStream outputStream)
            throws Exception {

        Document document = new Document(
                PageSize.A4.rotate(),
                25,
                25,
                30,
                30
        );

        PdfWriter.getInstance(document, outputStream);

        document.open();

        // =====================================================
        // TITLE
        // =====================================================

        Font titleFont = new Font(
                Font.FontFamily.HELVETICA,
                18,
                Font.BOLD
        );

        Paragraph titleParagraph =
                new Paragraph(title, titleFont);

        titleParagraph.setAlignment(Element.ALIGN_CENTER);

        document.add(titleParagraph);

        document.add(new Paragraph(" "));

        // =====================================================
        // EMPTY REPORT
        // =====================================================

        if (rows == null || rows.isEmpty()) {

            Font normalFont = new Font(
                    Font.FontFamily.HELVETICA,
                    11
            );

            Paragraph empty =
                    new Paragraph(
                            "No records were found for this report.",
                            normalFont
                    );

            empty.setAlignment(Element.ALIGN_CENTER);

            document.add(empty);

            document.close();

            return;
        }

        // =====================================================
        // GET COLUMNS FROM FIRST ROW
        // =====================================================

        List<String> columns =
                new ArrayList<>(rows.get(0).keySet());

        PdfPTable table =
                new PdfPTable(columns.size());

        table.setWidthPercentage(100);

        // =====================================================
        // HEADER
        // =====================================================

        Font headerFont = new Font(
                Font.FontFamily.HELVETICA,
                9,
                Font.BOLD
        );

        for (String column : columns) {

            PdfPCell cell =
                    new PdfPCell(
                            new Phrase(
                                    formatColumnName(column),
                                    headerFont
                            )
                    );

            cell.setHorizontalAlignment(
                    Element.ALIGN_CENTER
            );

            cell.setPadding(5);

            table.addCell(cell);
        }

        // =====================================================
        // DATA
        // =====================================================

        Font dataFont = new Font(
                Font.FontFamily.HELVETICA,
                8
        );

        for (Map<String, Object> row : rows) {

            for (String column : columns) {

                Object value = row.get(column);

                String text = formatValue(value);

                PdfPCell cell =
                        new PdfPCell(
                                new Phrase(
                                        text,
                                        dataFont
                                )
                        );

                cell.setPadding(4);

                table.addCell(cell);
            }
        }

        document.add(table);

        document.close();
    }

    // =========================================================
    // FORMAT COLUMN NAME
    // =========================================================

    private String formatColumnName(String column) {

        if (column == null || column.isBlank()) {
            return "";
        }

        StringBuilder result =
                new StringBuilder();

        for (int i = 0; i < column.length(); i++) {

            char c = column.charAt(i);

            if (i > 0 && Character.isUpperCase(c)) {
                result.append(' ');
            }

            result.append(c);
        }

        return result.toString();
    }

    // =========================================================
    // FORMAT DATABASE VALUE
    // =========================================================

    private String formatValue(Object value) {

        if (value == null) {
            return "";
        }

        if (value instanceof Number) {

            return decimalFormat.format(
                    ((Number) value).doubleValue()
            );
        }

        if (value instanceof Boolean) {

            return ((Boolean) value)
                    ? "Yes"
                    : "No";
        }

        return value.toString();
    }
}