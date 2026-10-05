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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Builds the PDF in memory with iText from the same rows the web page shows. */
@Service
public class PdfExportService {

    public void export(String title, List<Map<String, Object>> rows, OutputStream out) throws Exception {

        Document document = new Document(PageSize.A4.rotate(), 25, 25, 30, 30);
        PdfWriter.getInstance(document, out);
        document.open();

        Paragraph heading = new Paragraph(title, new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD));
        heading.setAlignment(Element.ALIGN_CENTER);
        document.add(heading);

        Paragraph generated = new Paragraph(
                "LankaTech Computer Supplies (Pvt) Ltd - generated "
                        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                new Font(Font.FontFamily.HELVETICA, 9));
        generated.setAlignment(Element.ALIGN_CENTER);
        document.add(generated);
        document.add(new Paragraph(" "));

        if (rows == null || rows.isEmpty()) {
            Paragraph empty = new Paragraph("No records were found for this report.",
                    new Font(Font.FontFamily.HELVETICA, 11));
            empty.setAlignment(Element.ALIGN_CENTER);
            document.add(empty);
            document.close();
            return;
        }

        List<String> columns = new ArrayList<>(rows.get(0).keySet());
        PdfPTable table = new PdfPTable(columns.size());
        table.setWidthPercentage(100);
        table.setHeaderRows(1);

        Font headerFont = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);
        for (String column : columns) {
            PdfPCell cell = new PdfPCell(new Phrase(column, headerFont));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }

        Font dataFont = new Font(Font.FontFamily.HELVETICA, 8);
        for (Map<String, Object> row : rows) {
            for (String column : columns) {
                Object v = row.get(column);
                PdfPCell cell = new PdfPCell(new Phrase(v == null ? "" : v.toString(), dataFont));
                cell.setPadding(4);
                table.addCell(cell);
            }
        }

        document.add(table);
        document.close();
    }
}
