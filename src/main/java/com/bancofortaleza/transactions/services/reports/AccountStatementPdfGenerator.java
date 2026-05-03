package com.bancofortaleza.transactions.services.reports;

import com.bff.services.server.models.AccountStatementAccount;
import com.bff.services.server.models.AccountStatementReportResponse;
import com.bff.services.server.models.AccountStatementTransaction;
import com.bff.services.server.models.ConceptTransaction;
import com.bff.services.server.models.Status;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Phrase;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class AccountStatementPdfGenerator {

    private static final DateTimeFormatter REPORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public byte[] generate(AccountStatementReportResponse report) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.LETTER, 36, 36, 36, 36);
            PdfWriter.getInstance(document, outputStream);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font textFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font debitFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.RED);

            Paragraph title = new Paragraph("BANCO FORTALEZA FINANCIERA", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            document.add(new Paragraph("Estado de cuenta", titleFont));
            document.add(new Paragraph("Cliente: " + report.getIdUser(), textFont));
            document.add(new Paragraph(
                "Periodo: " + formatStartDate(report.getStartDate()) + " al " + formatEndDate(report.getEndDate()),
                textFont
            ));
            document.add(new Paragraph("Generado: " + formatDate(report.getGeneratedAt()), textFont));
            document.add(new Paragraph("Total debitos: " + amount(report.getTotalDebits()), textFont));
            document.add(new Paragraph("Total creditos: " + amount(report.getTotalCredits()), textFont));
            document.add(new Paragraph(" "));

            for (int index = 0; index < report.getAccounts().size(); index++) {
                AccountStatementAccount account = report.getAccounts().get(index);
                if (index > 0) {
                    document.newPage();
                }
                document.add(new Paragraph("Cuenta " + account.getAccountNumber(), sectionFont));
                document.add(new Paragraph("Saldo: " + amount(account.getBalance()), textFont));
                document.add(new Paragraph("Total debitos: " + amount(account.getTotalDebits()), textFont));
                document.add(new Paragraph("Total creditos: " + amount(account.getTotalCredits()), textFont));
                document.add(new Paragraph("Transacciones:", sectionFont));
                document.add(new Paragraph(" "));

                if (account.getTransactions().isEmpty()) {
                    document.add(new Paragraph("Sin movimientos en el periodo.", textFont));
                } else {
                    document.add(buildTransactionsTable(account, textFont, debitFont));
                }
                document.add(new Paragraph(" "));
            }

            document.close();
            return outputStream.toByteArray();
        } catch (DocumentException | IOException exception) {
            throw new IllegalStateException("Could not generate account statement PDF", exception);
        }
    }

    private PdfPTable buildTransactionsTable(
        AccountStatementAccount account,
        Font textFont,
        Font debitFont
    ) {
        PdfPTable table = new PdfPTable(new float[] {2.1f, 1.2f, 1.2f, 4.0f});
        table.setWidthPercentage(100);
        addHeaderCell(table, "Fecha");
        addHeaderCell(table, "Concepto");
        addHeaderCell(table, "Monto");
        addHeaderCell(table, "Descripcion");

        for (AccountStatementTransaction transaction : account.getTransactions()) {
            addCell(table, formatDate(transaction), textFont);
            addCell(table, conceptLabel(transaction), textFont);
            addCell(table, transactionAmount(transaction), amountFont(transaction, textFont, debitFont));
            addCell(table, description(transaction), textFont);
        }

        return table;
    }

    private void addHeaderCell(PdfPTable table, String value) {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
        PdfPCell cell = new PdfPCell(new Phrase(value, headerFont));
        cell.setBackgroundColor(new Color(230, 230, 230));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String value, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(value, font));
        cell.setPadding(5);
        table.addCell(cell);
    }

    private String conceptLabel(AccountStatementTransaction transaction) {
        return isDebit(transaction) ? "Debito" : "Credito";
    }

    private String transactionAmount(AccountStatementTransaction transaction) {
        return isDebit(transaction)
            ? "(" + amount(transaction.getAmount()) + ")"
            : amount(transaction.getAmount());
    }

    private Font amountFont(AccountStatementTransaction transaction, Font textFont, Font debitFont) {
        return isDebit(transaction) ? debitFont : textFont;
    }

    private boolean isDebit(AccountStatementTransaction transaction) {
        return transaction.getConcept() == ConceptTransaction.DEBIT;
    }

    private String statusLabel(AccountStatementTransaction transaction) {
        return transaction.getStatus() == Status.INACTIVE ? "Op cancelada" : "";
    }

    private String description(AccountStatementTransaction transaction) {
        String description = nullSafe(transaction.getDescription());
        String status = statusLabel(transaction);
        return status.isBlank() ? description : description + " (" + status + ")";
    }

    private String formatDate(AccountStatementTransaction transaction) {
        return transaction.getTransactionDate() == null
            ? ""
            : formatDate(transaction.getTransactionDate());
    }

    private String formatStartDate(LocalDate value) {
        return value == null ? "" : formatDate(value.atStartOfDay().atOffset(ZoneOffset.UTC));
    }

    private String formatEndDate(LocalDate value) {
        return value == null ? "" : formatDate(value.atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC));
    }

    private String formatDate(OffsetDateTime value) {
        return value == null ? "" : value.format(REPORT_DATE_FORMATTER);
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private String amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.toPlainString() : value.toPlainString();
    }
}
