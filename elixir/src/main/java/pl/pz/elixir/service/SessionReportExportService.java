package pl.pz.elixir.service;

import org.springframework.stereotype.Service;
import pl.pz.elixir.model.SessionReport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class SessionReportExportService {

    public byte[] exportSessionReportDocx(SessionReport report) {
        List<String> lines = report == null || report.getNettingResult() == null
                ? List.of()
                : report.getNettingResult();
        String sessionName = report == null ? null : report.getSessionName();
        LocalDateTime closedAt = report == null ? null : report.getClosedAt();
        return buildDocx("Raport sesji ELIXIR", sessionName, closedAt, lines);
    }

    public byte[] exportBankReportDocx(String bankId, SessionReport report, List<String> bankLines) {
        String sessionName = report == null ? null : report.getSessionName();
        LocalDateTime closedAt = report == null ? null : report.getClosedAt();
        return buildDocx("Raport banku " + safe(bankId), sessionName, closedAt, bankLines == null ? List.of() : bankLines);
    }

    public byte[] exportSessionReportPdf(SessionReport report) {
        List<String> lines = report == null || report.getNettingResult() == null
                ? List.of()
                : report.getNettingResult();
        String sessionName = report == null ? null : report.getSessionName();
        LocalDateTime closedAt = report == null ? null : report.getClosedAt();
        return buildPdf("Raport sesji ELIXIR", sessionName, closedAt, lines);
    }

    public byte[] exportBankReportPdf(String bankId, SessionReport report, List<String> bankLines) {
        String sessionName = report == null ? null : report.getSessionName();
        LocalDateTime closedAt = report == null ? null : report.getClosedAt();
        return buildPdf("Raport banku " + safe(bankId), sessionName, closedAt, bankLines == null ? List.of() : bankLines);
    }

    private byte[] buildDocx(String title, String sessionName, LocalDateTime closedAt, List<String> lines) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
                addZipEntry(zip, "[Content_Types].xml", contentTypesXml());
                addZipEntry(zip, "_rels/.rels", relsXml());
                addZipEntry(zip, "word/document.xml", documentXml(title, sessionName, closedAt, lines));
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Nie można wygenerować raportu DOCX: " + e.getMessage(), e);
        }
    }

    private String documentXml(String title, String sessionName, LocalDateTime closedAt, List<String> lines) {
        StringBuilder body = new StringBuilder();
        body.append(paragraph(title, true));
        body.append(paragraph("Sesja: " + safe(sessionName), false));
        body.append(paragraph("Zamknięta: " + (closedAt == null ? "-" : closedAt.toString()), false));
        body.append(paragraph("", false));

        if (lines == null || lines.isEmpty()) {
            body.append(paragraph("Brak pozycji w raporcie.", false));
        } else {
            for (String line : lines) {
                body.append(paragraph("• " + safe(line), false));
            }
        }

        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\">"
                + "<w:body>"
                + body
                + "<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/><w:pgMar w:top=\"1440\" w:right=\"1440\" w:bottom=\"1440\" w:left=\"1440\"/></w:sectPr>"
                + "</w:body></w:document>";
    }

    private String paragraph(String text, boolean title) {
        String style = title
                ? "<w:rPr><w:b/><w:sz w:val=\"32\"/></w:rPr>"
                : "<w:rPr><w:sz w:val=\"22\"/></w:rPr>";
        return "<w:p><w:r>" + style + "<w:t xml:space=\"preserve\">" + escapeXml(text) + "</w:t></w:r></w:p>";
    }

    private String contentTypesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
                + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
                + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
                + "<Override PartName=\"/word/document.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/>"
                + "</Types>";
    }

    private String relsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
                + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"word/document.xml\"/>"
                + "</Relationships>";
    }

    private void addZipEntry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private byte[] buildPdf(String title, String sessionName, LocalDateTime closedAt, List<String> lines) {
        List<String> allLines = new ArrayList<>();
        allLines.add(title);
        allLines.add("Sesja: " + safe(sessionName));
        allLines.add("Zamknieta: " + (closedAt == null ? "-" : closedAt.toString()));
        allLines.add("");
        if (lines == null || lines.isEmpty()) {
            allLines.add("Brak pozycji w raporcie.");
        } else {
            for (String line : lines) {
                allLines.add("- " + safe(line));
            }
        }

        List<List<String>> pages = splitLines(allLines, 48);
        int pageCount = pages.size();
        int fontObjId = 3 + pageCount * 2;

        StringBuilder pdf = new StringBuilder();
        List<Integer> offsets = new ArrayList<>();
        pdf.append("%PDF-1.4\n");

        addObject(pdf, offsets, 1, "<< /Type /Catalog /Pages 2 0 R >>");

        StringBuilder kids = new StringBuilder();
        for (int i = 0; i < pageCount; i++) {
            kids.append(3 + i * 2).append(" 0 R ");
        }
        addObject(pdf, offsets, 2, "<< /Type /Pages /Kids [" + kids + "] /Count " + pageCount + " >>");

        for (int i = 0; i < pageCount; i++) {
            int pageObjId = 3 + i * 2;
            int contentObjId = pageObjId + 1;
            addObject(pdf, offsets, pageObjId,
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 "
                            + fontObjId + " 0 R >> >> /Contents " + contentObjId + " 0 R >>");
            String content = pdfPageContent(pages.get(i));
            addStreamObject(pdf, offsets, contentObjId, content);
        }

        addObject(pdf, offsets, fontObjId, "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");

        int xrefOffset = pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length;
        int objectCount = fontObjId;
        pdf.append("xref\n0 ").append(objectCount + 1).append("\n");
        pdf.append("0000000000 65535 f \n");
        for (int offset : offsets) {
            pdf.append(String.format(Locale.US, "%010d 00000 n \n", offset));
        }
        pdf.append("trailer\n<< /Size ").append(objectCount + 1).append(" /Root 1 0 R >>\n");
        pdf.append("startxref\n").append(xrefOffset).append("\n%%EOF");

        return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private List<List<String>> splitLines(List<String> lines, int perPage) {
        List<List<String>> pages = new ArrayList<>();
        for (int i = 0; i < lines.size(); i += perPage) {
            pages.add(lines.subList(i, Math.min(i + perPage, lines.size())));
        }
        if (pages.isEmpty()) {
            pages.add(List.of("Brak pozycji w raporcie."));
        }
        return pages;
    }

    private String pdfPageContent(List<String> lines) {
        StringBuilder content = new StringBuilder();
        content.append("BT\n/F1 11 Tf\n50 800 Td\n14 TL\n");
        for (String line : lines) {
            content.append("(").append(escapePdfText(toPdfAscii(line))).append(") Tj\nT*\n");
        }
        content.append("ET\n");
        return content.toString();
    }

    private void addObject(StringBuilder pdf, List<Integer> offsets, int objId, String body) {
        offsets.add(pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length);
        pdf.append(objId).append(" 0 obj\n").append(body).append("\nendobj\n");
    }

    private void addStreamObject(StringBuilder pdf, List<Integer> offsets, int objId, String stream) {
        byte[] bytes = stream.getBytes(StandardCharsets.ISO_8859_1);
        offsets.add(pdf.toString().getBytes(StandardCharsets.ISO_8859_1).length);
        pdf.append(objId).append(" 0 obj\n")
                .append("<< /Length ").append(bytes.length).append(" >>\n")
                .append("stream\n")
                .append(stream)
                .append("endstream\nendobj\n");
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String escapeXml(String value) {
        return safe(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String escapePdfText(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private String toPdfAscii(String value) {
        return safe(value)
                .replace("ą", "a").replace("ć", "c").replace("ę", "e").replace("ł", "l")
                .replace("ń", "n").replace("ó", "o").replace("ś", "s").replace("ź", "z").replace("ż", "z")
                .replace("Ą", "A").replace("Ć", "C").replace("Ę", "E").replace("Ł", "L")
                .replace("Ń", "N").replace("Ó", "O").replace("Ś", "S").replace("Ź", "Z").replace("Ż", "Z")
                .replace("→", "->")
                .replace("–", "-")
                .replace("—", "-")
                .replace("•", "-");
    }
}