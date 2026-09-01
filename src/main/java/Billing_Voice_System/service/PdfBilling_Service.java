package Billing_Voice_System.service;
import com.itextpdf.kernel.colors.DeviceRgb;
import Billing_Voice_System.dto.FinalBillDto;
import Billing_Voice_System.dto.ItemDto;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;


import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PdfBilling_Service {
    // Modern Color Palette
    private static final Color PRIMARY_COLOR = new DeviceRgb(79, 70, 229);    // Indigo Accent #4F46E5
    private static final Color HEADER_BG = new DeviceRgb(243, 244, 246);       // Light Gray #F3F4F6
    private static final Color ROW_ALT_BG = new DeviceRgb(249, 250, 251);      // Subtle Row Shading #F9FAFB
    private static final Color TEXT_DARK = new DeviceRgb(17, 24, 39);          // Slate Dark #111827
    private static final Color TEXT_MUTED = new DeviceRgb(107, 114, 128);      // Muted Gray #6B7280
    private static final Color BORDER_COLOR = new DeviceRgb(229, 231, 235);    // Divider Gray #E5E7EB

    public byte[] generatepdfBill(FinalBillDto billDto) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(outputStream);
        PdfDocument pdfDocument = new PdfDocument(writer);
        pdfDocument.setDefaultPageSize(PageSize.A4);

        Document doc = new Document(pdfDocument);
        doc.setMargins(36f, 36f, 36f, 36f);

        try {
            // 1. Top Header Bar
            Table headerTable = new Table(UnitValue.createPercentArray(new float[]{60f, 40f}))
                    .useAllAvailableWidth()
                    .setBorder(Border.NO_BORDER);

            Cell brandCell = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .add(new Paragraph("AI POWERED BILLING")
                            .setFontSize(20)
                            .setBold()
                            .setFontColor(PRIMARY_COLOR))
                    .add(new Paragraph("Voice-Automated Invoice Receipt")
                            .setFontSize(10)
                            .setFontColor(TEXT_MUTED));
            headerTable.addCell(brandCell);

            String formattedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"));
            Cell metaCell = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .add(new Paragraph("TAX INVOICE")
                            .setFontSize(14)
                            .setBold()
                            .setFontColor(TEXT_DARK))
                    .add(new Paragraph("Date: " + formattedDate)
                            .setFontSize(9)
                            .setFontColor(TEXT_MUTED));
            headerTable.addCell(metaCell);

            doc.add(headerTable);

            doc.add(new Paragraph("")
                    .setBorderBottom(new SolidBorder(BORDER_COLOR, 1.5f))
                    .setMarginTop(12f)
                    .setMarginBottom(18f));

            // 2. Items Table
            float[] columnWidths = {42f, 18f, 20f, 20f};
            Table itemTable = new Table(UnitValue.createPercentArray(columnWidths))
                    .useAllAvailableWidth()
                    .setBorder(new SolidBorder(BORDER_COLOR, 1f));

            String[] headers = {"ITEM DESCRIPTION", "QTY & UNIT", "UNIT PRICE", "AMOUNT"};
            TextAlignment[] alignments = {TextAlignment.LEFT, TextAlignment.CENTER, TextAlignment.RIGHT, TextAlignment.RIGHT};

            for (int i = 0; i < headers.length; i++) {
                Cell headerCell = new Cell()
                        .setBackgroundColor(HEADER_BG)
                        .setPaddingTop(8f)
                        .setPaddingBottom(8f)
                        .setPaddingLeft(10f)
                        .setPaddingRight(10f)
                        .setBorder(new SolidBorder(BORDER_COLOR, 1f))
                        .setTextAlignment(alignments[i])
                        .add(new Paragraph(headers[i])
                                .setFontSize(9)
                                .setBold()
                                .setFontColor(TEXT_DARK));
                itemTable.addCell(headerCell);
            }

            if (billDto.getItems() != null && !billDto.getItems().isEmpty()) {
                int index = 0;
                for (ItemDto item : billDto.getItems()) {
                    Color rowBg = (index % 2 == 0) ? ColorConstants.WHITE : ROW_ALT_BG;

                    String unitLabel = (item.getUnit() != null && !item.getUnit().isBlank()) ? " " + item.getUnit() : "";
                    String qtyStr = formatNumber(item.getQty()) + unitLabel;
                    String priceStr = "₹ " + String.format("%.2f", item.getPrice());

                    double itemTotal = item.getQty() * item.getPrice();
                    String totalStr = "₹ " + String.format("%.2f", itemTotal);

                    itemTable.addCell(createDataCell(capitalize(item.getName()), TextAlignment.LEFT, rowBg).setBold());
                    itemTable.addCell(createDataCell(qtyStr, TextAlignment.CENTER, rowBg));
                    itemTable.addCell(createDataCell(priceStr, TextAlignment.RIGHT, rowBg));
                    itemTable.addCell(createDataCell(totalStr, TextAlignment.RIGHT, rowBg));

                    index++;
                }
            }

            doc.add(itemTable);

            // 3. Summary & Grand Total Section
            Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{60f, 40f}))
                    .useAllAvailableWidth()
                    .setMarginTop(15f)
                    .setBorder(Border.NO_BORDER);

            Cell noteCell = new Cell()
                    .setBorder(Border.NO_BORDER)
                    .add(new Paragraph("Payment Note:")
                            .setFontSize(9)
                            .setBold()
                            .setFontColor(TEXT_DARK))
                    .add(new Paragraph("This is a computer-generated invoice created via speech parsing. No signature required.")
                            .setFontSize(8)
                            .setFontColor(TEXT_MUTED)
                            .setMaxWidth(UnitValue.createPointValue(240f)));
            summaryTable.addCell(noteCell);

            Cell grandTotalCell = new Cell()
                    .setBackgroundColor(HEADER_BG)
                    .setBorder(new SolidBorder(BORDER_COLOR, 1f))
                    .setPadding(12f)
                    .setTextAlignment(TextAlignment.RIGHT)
                    .add(new Paragraph("TOTAL AMOUNT PAYABLE")
                            .setFontSize(9)
                            .setBold()
                            .setFontColor(TEXT_MUTED))
                    .add(new Paragraph("₹ " + String.format("%.2f", billDto.getTotal()))
                            .setFontSize(18)
                            .setBold()
                            .setFontColor(PRIMARY_COLOR));
            summaryTable.addCell(grandTotalCell);

            doc.add(summaryTable);

            // 4. Footer
            Paragraph footer = new Paragraph("Thank you for your business!")
                    .setFontSize(10)
                    .setFontColor(TEXT_MUTED)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(40f);
            doc.add(footer);

        } finally {
            doc.close();
        }

        return outputStream.toByteArray();
    }

    private Cell createDataCell(String text, TextAlignment alignment, Color bgColor) {
        return new Cell()
                .setBackgroundColor(bgColor)
                .setBorder(new SolidBorder(BORDER_COLOR, 1f))
                .setPaddingTop(8f)
                .setPaddingBottom(8f)
                .setPaddingLeft(10f)
                .setPaddingRight(10f)
                .setTextAlignment(alignment)
                .add(new Paragraph(text != null ? text : "-")
                        .setFontSize(9)
                        .setFontColor(TEXT_DARK));
    }

    private String formatNumber(double value) {
        if (value == (long) value) {
            return String.format("%d", (long) value);
        }
        return String.format("%.2f", value);
    }

    private String capitalize(String text) {
        if (text == null || text.isBlank()) return "-";
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }




    
}
