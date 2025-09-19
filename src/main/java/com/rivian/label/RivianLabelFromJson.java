package com.rivian.label;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;

public class RivianLabelFromJson {

    private static final float INCH = 72f; // 1 inch = 72 pts

    public static void main(String[] args) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new File("pdf-qr-generation-from-json-main/container-label-generator/src/input.json")).get("container_label");

            float pageWidth = 6 * INCH;  // 432 pts
            float pageHeight = 4 * INCH; // 288 pts
            float margin = 10f;

            PDDocument doc = new PDDocument();
            PDPage page = new PDPage(new org.apache.pdfbox.pdmodel.common.PDRectangle(pageWidth, pageHeight));
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            // Row heights (4 rows)
            float[] rowHeights = {(pageHeight - 2*margin)/4f, (pageHeight - 2*margin)/4f, (pageHeight - 2*margin)/4f, (pageHeight - 2*margin)/4f};

            // Column positions
            float colWidth = (pageWidth - 2*margin)/3f;
            float col1X = margin;
            float col2X = margin + colWidth;
            float col3X = margin + 2*colWidth;

            // -------- Draw Row 1: 3 columns --------
            drawGrid(cs, margin, pageHeight - margin - rowHeights[0], pageWidth - 2*margin, rowHeights[0], 3);
            // Draw Ship From with font size 0.08 inches (5.76 pts)
            drawAddress(cs, root.get("ship_from"), "Ship From", col1X, pageHeight - margin - rowHeights[0], colWidth, rowHeights[0], (int)(0.08f * INCH));
            // Draw Ship To with font size 0.10 inches (7.2 pts)
            drawAddress(cs, root.get("ship_to"), "Ship To", col2X, pageHeight - margin - rowHeights[0], colWidth, rowHeights[0], (int)(0.10f * INCH));
        drawQRCode(cs, doc, root.get("qr_code").get("encoded_string").asText(),
            col3X + 20, pageHeight - margin - rowHeights[0] + 10, 0.75f*INCH, 0.75f*INCH);

            // -------- Draw Row 2: 2 columns (left 2/3, right 1/3) --------
                drawCustomGrid(cs, margin, pageHeight - margin - rowHeights[0] - rowHeights[1], new float[]{2*colWidth, colWidth}, rowHeights[1]);
        // Custom rendering for P/N with value in larger font, left aligned, and (P) below
        float pnFontSize = 0.12f * INCH; // 8.64 pts
        float pnBlockX = col1X;
        float pnBlockY = pageHeight - margin - rowHeights[0] - rowHeights[1];
        float pnBlockW = 2*colWidth;
        float pnBlockH = rowHeights[1];
        float pnPaddingLeft = 0.25f * INCH; // 0.25 inch gap to left of barcode
        float pnLabelPadding = 8f; // move P/N label a little left
        float pnLineHeight = 6 + 2;


    // Draw 'P/N' and part number value on the same line, with part number centered and larger
    float pnValueGap = 60f; // increased gap to move part number to center
    float pnTextY = pnBlockY + pnBlockH - pnLineHeight - 3f;
    float pnValueFontSize = 0.18f * INCH; // increase size to 13 pts
    cs.beginText();
    cs.setFont(PDType1Font.HELVETICA, 8); // unbold P/N
    cs.newLineAtOffset(pnBlockX + pnLabelPadding, pnTextY);
    cs.showText("P/N");
    cs.setFont(PDType1Font.HELVETICA_BOLD, pnValueFontSize);
    cs.newLineAtOffset(pnValueGap, -10f); // move part number value 10 pts down
    cs.showText(root.get("partNumber").asText());
    cs.endText();

    // Draw (P) just below P/N, bold
    cs.beginText();
    cs.setFont(PDType1Font.HELVETICA_BOLD, 7);
    cs.newLineAtOffset(pnBlockX + pnLabelPadding, pnTextY - pnLineHeight - 2f);
    cs.showText("(P)");
    cs.endText();

        // Draw barcode for part number with 0.25 inch gap to left
        String pnBarcodeData = root.get("partNumber").asText();
        float pnBarcodeHeight = 0.5f * INCH;
        float pnBarcodeWidth = pnBlockW - pnPaddingLeft - 2*0.25f*INCH;
        BitMatrix pnMatrix = new MultiFormatWriter().encode(pnBarcodeData, BarcodeFormat.CODE_128,
            (int)pnBarcodeWidth, (int)pnBarcodeHeight);
        BufferedImage pnImage = MatrixToImageWriter.toBufferedImage(pnMatrix);
        PDImageXObject pnPdImage = PDImageXObject.createFromByteArray(doc, toByteArray(pnImage), "barcode");
        cs.drawImage(pnPdImage, pnBlockX + pnPaddingLeft, pnBlockY + 3f, pnBarcodeWidth, pnBarcodeHeight);

        // Custom rendering for QTY with value in larger font
        float qtyFontSize = 0.12f * INCH; // 8.64 pts
        float qtyBlockX = col3X;
        float qtyBlockY = pageHeight - margin - rowHeights[0] - rowHeights[1];
        float qtyBlockW = colWidth;
        float qtyBlockH = rowHeights[1];
        float padding = 3f;
        float lineHeight = 6 + 2;



    // Draw 'QTY' and quantity value on the same line
    float qtyValueFontSize = 13f; // larger font size
    float qtyValueGap = 28f; // move value further right
    float qtyTextY = qtyBlockY + qtyBlockH - lineHeight - padding;
    cs.beginText();
    cs.setFont(PDType1Font.HELVETICA, 6);
    cs.newLineAtOffset(qtyBlockX + padding + 10, qtyTextY);
    cs.showText("QTY");
    cs.setFont(PDType1Font.HELVETICA_BOLD, qtyValueFontSize);
    cs.newLineAtOffset(qtyValueGap, -8f); // move value 8 pts down
    cs.showText(root.get("quantity").asText());
    cs.endText();

    // Draw (Q) just below QTY, bold
    cs.beginText();
    cs.setFont(PDType1Font.HELVETICA_BOLD, 7);
    cs.newLineAtOffset(qtyBlockX + padding + 10, qtyTextY - lineHeight - 2f);
    cs.showText("(Q)");
    cs.endText();

        // Draw barcode for quantity
        String qtyBarcodeData = root.get("quantity").asText();
        float barcodeHeight = 0.5f * INCH;
        float barcodeWidth = qtyBlockW - 2*padding - 2*0.25f*INCH;
        BitMatrix matrix = new MultiFormatWriter().encode(qtyBarcodeData, BarcodeFormat.CODE_128,
            (int)barcodeWidth, (int)barcodeHeight);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
        PDImageXObject pdImage = PDImageXObject.createFromByteArray(doc, toByteArray(image), "barcode");
        cs.drawImage(pdImage, qtyBlockX + padding, qtyBlockY + padding, barcodeWidth, barcodeHeight);

            // -------- Draw Row 3: 2 columns --------
                drawCustomGrid(cs, margin, pageHeight - margin - rowHeights[0] - rowHeights[1] - rowHeights[2], new float[]{2*colWidth, colWidth}, rowHeights[2]);
        // Custom rendering for Description block (row 3 col 1)
        float descBlockX = col1X;
        float descBlockY = pageHeight - margin - rowHeights[0] - rowHeights[1] - rowHeights[2];
        float descBlockW = 2*colWidth;
        float descBlockH = rowHeights[2];
        float descLabelFontSize = 8f;
        float descValueFontSize = 0.15f * INCH; // 10.8 pts
        float descPadding = 3f;

        // Bold label 'Description' at top left
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, descLabelFontSize);
        cs.newLineAtOffset(descBlockX + descPadding, descBlockY + descBlockH - descLabelFontSize - descPadding);
        cs.showText("Description");
        cs.endText();

        // Center value in block, both horizontally and vertically
        String descValue = root.get("partDescription").asText();
        float valueWidth = descValue.length() * descValueFontSize * 0.6f; // rough estimate
        float valueX = descBlockX + (descBlockW - valueWidth) / 2f;
        float valueY = descBlockY + (descBlockH - descValueFontSize) / 2f;
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, descValueFontSize);
        cs.newLineAtOffset(valueX, valueY);
        cs.showText(descValue);
        cs.endText();
            // Custom rendering for row 3 col 2 info block: keys with values below each key
            float infoBlockX = col3X;
            float infoBlockY = pageHeight - margin - rowHeights[0] - rowHeights[1] - rowHeights[2];
            float infoBlockW = colWidth;
            float infoBlockH = rowHeights[2];
            float infoFontSizePts = 0.08f * INCH; // 0.08 inches -> 5.76 pts
            float infoPadding = 4f; // even less horizontal padding
            float infoColGap = 40f; // even less gap between columns
            float infoLineHeight = infoFontSizePts + 1.5f; // even less vertical gap between rows
            float infoKeyValueGap = 1f; // even less gap between key and value
            // column x positions: three columns with horizontal gaps
            float infoCol1X = infoBlockX + infoPadding;
            float infoCol2X = infoCol1X + infoColGap;
            float infoCol3X = infoCol2X + infoColGap;

            // PO No.
            float yBase = infoBlockY + infoBlockH - infoPadding - infoFontSizePts;

            // PO NO.
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, infoFontSizePts);
            cs.newLineAtOffset(infoCol1X, yBase);
            cs.showText("PO NO.");
            cs.endText();
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, infoFontSizePts);
            cs.newLineAtOffset(infoCol1X, yBase - infoFontSizePts - infoKeyValueGap);
            cs.showText(root.get("poNumber").asText());
            cs.endText();

            // PO LINE NO.
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, infoFontSizePts);
            cs.newLineAtOffset(infoCol2X, yBase);
            cs.showText("PO LINE NO.");
            cs.endText();
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, infoFontSizePts);
            cs.newLineAtOffset(infoCol2X, yBase - infoFontSizePts - infoKeyValueGap);
            cs.showText(root.get("poLineNumber").asText());
            cs.endText();

            // PROD DATE
            float yProd = yBase - 2 * (infoFontSizePts + infoKeyValueGap) - infoLineHeight;
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, infoFontSizePts);
            cs.newLineAtOffset(infoCol1X, yProd);
            cs.showText("PROD DATE");
            cs.endText();
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, infoFontSizePts);
            cs.newLineAtOffset(infoCol1X, yProd - infoFontSizePts - infoKeyValueGap);
            cs.showText(root.get("productionDate").asText());
            cs.endText();

            // EXP DATE
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, infoFontSizePts);
            cs.newLineAtOffset(infoCol2X, yProd);
            cs.showText("EXP DATE");
            cs.endText();
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, infoFontSizePts);
            cs.newLineAtOffset(infoCol2X, yProd - infoFontSizePts - infoKeyValueGap);
            cs.showText(root.get("expirationDate").asText());
            cs.endText();

            // LOT NO., QML, PCD (last line, more gap between columns)
            float yLot = yProd - 2 * (infoFontSizePts + infoKeyValueGap) - infoLineHeight;
            float lastColGap1 = infoColGap + 20f; // gap between LOT NO. and QML
            float lastColGap2 = infoColGap + 8f;  // smaller gap between QML and PCD

            float lotX = infoCol1X;
            float qmlX = lotX + lastColGap1;
            float pcdX = qmlX + lastColGap2;

            // LOT NO.
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, infoFontSizePts);
            cs.newLineAtOffset(lotX, yLot);
            cs.showText("LOT NO.");
            cs.endText();
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, infoFontSizePts);
            cs.newLineAtOffset(lotX, yLot - infoFontSizePts - infoKeyValueGap);
            cs.showText(root.get("lotNumber").asText());
            cs.endText();

            // QML
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, infoFontSizePts);
            cs.newLineAtOffset(qmlX, yLot);
            cs.showText("QML");
            cs.endText();
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, infoFontSizePts);
            cs.newLineAtOffset(qmlX, yLot - infoFontSizePts - infoKeyValueGap);
            cs.showText(root.get("qml").asText());
            cs.endText();

            // PCD
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, infoFontSizePts);
            cs.newLineAtOffset(pcdX, yLot);
            cs.showText("PCD");
            cs.endText();
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, infoFontSizePts);
            cs.newLineAtOffset(pcdX, yLot - infoFontSizePts - infoKeyValueGap);
            cs.showText(root.get("pcd").asText());
            cs.endText();

            // -------- Draw Row 4: 2 columns --------
                drawCustomGrid(cs, margin, pageHeight - margin - rowHeights[0] - rowHeights[1] - rowHeights[2] - rowHeights[3], new float[]{2*colWidth, colWidth}, rowHeights[3]);
            // Custom rendering for row 4 col 1: remove LPN, keep bold (1J), value above barcode, size 0.12 inches bold
            float lpnBlockX = col1X;
            float lpnBlockY = pageHeight - margin - rowHeights[0] - rowHeights[1] - rowHeights[2] - rowHeights[3];
            float lpnBlockW = 2*colWidth;
            float lpnBlockH = rowHeights[3];
            float lpnPadding = 8f;
            float lpnLabelFontSize = 8f;
            float lpnValueFontSize = 0.12f * INCH; // 8.64 pts
            float lpnLineHeight = lpnLabelFontSize + 2;

            // Draw bold (1J) at top left
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, lpnLabelFontSize);
            cs.newLineAtOffset(lpnBlockX + lpnPadding, lpnBlockY + lpnBlockH - lpnLineHeight - 3f);
            cs.showText("(1J)");
            cs.endText();

            // Draw value above barcode, centered horizontally, at 0.12 inches (8.64 pts) bold
            String lpnValue = root.get("lpn_1j").asText();
            float lpnValueWidth = lpnValue.length() * lpnValueFontSize * 0.6f; // rough estimate
            float lpnValueX = lpnBlockX + (lpnBlockW - lpnValueWidth) / 2f;
            float lpnValueY = lpnBlockY + lpnBlockH/2f + lpnValueFontSize;
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, lpnValueFontSize);
            cs.newLineAtOffset(lpnValueX, lpnValueY);
            cs.showText(lpnValue);
            cs.endText();

            // Draw barcode for LPN value below
            String lpnBarcodeData = lpnValue;
            float lpnBarcodeHeight = 0.5f * INCH;
            float lpnBarcodeWidth = lpnBlockW - 2*lpnPadding - 2*0.25f*INCH;
            BitMatrix lpnMatrix = new MultiFormatWriter().encode(lpnBarcodeData, BarcodeFormat.CODE_128,
                (int)lpnBarcodeWidth, (int)lpnBarcodeHeight);
            BufferedImage lpnImage = MatrixToImageWriter.toBufferedImage(lpnMatrix);
            PDImageXObject lpnPdImage = PDImageXObject.createFromByteArray(doc, toByteArray(lpnImage), "barcode");
            cs.drawImage(lpnPdImage, lpnBlockX + lpnPadding, lpnBlockY + 3f, lpnBarcodeWidth, lpnBarcodeHeight);
            // 'Supplier internal' at top center, not bold
            float supBlockX = col3X;
            float supBlockY = pageHeight - margin - rowHeights[0] - rowHeights[1] - rowHeights[2] - rowHeights[3];
            float supBlockW = colWidth;
            float supBlockH = rowHeights[3];
            String supText = "Supplier internal";
            float supFontSize = 8f;
            float supTextWidth = supText.length() * supFontSize * 0.6f; // rough estimate
            float supTextX = supBlockX + (supBlockW - supTextWidth) / 2f;
            float supTextY = supBlockY + supBlockH - supFontSize - 6f; // near top
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, supFontSize);
            cs.newLineAtOffset(supTextX, supTextY);
            cs.showText(supText);
            cs.endText();

            cs.close();
            doc.save("rivian_label.pdf");
            doc.close();
            System.out.println("Label PDF generated successfully!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Helper Methods ---
    private static String getJsonText(JsonNode node, String fieldName) {
        return node.has(fieldName) && !node.get(fieldName).isNull() ? node.get(fieldName).asText() : "";
    }

    private static void drawGrid(PDPageContentStream cs, float x, float y, float w, float h, int cols) throws Exception {
        // Draw outer rectangle
        cs.addRect(x, y, w, h);
        cs.stroke();
        // Draw vertical lines for columns
        float colWidth = w / cols;
        for(int i=1; i<cols; i++){
            cs.moveTo(x + i*colWidth, y);
            cs.lineTo(x + i*colWidth, y + h);
            cs.stroke();
        }
    }

    // Draws a grid with custom column widths
    private static void drawCustomGrid(PDPageContentStream cs, float x, float y, float[] colWidths, float h) throws Exception {
        float w = 0f;
        for (float cw : colWidths) w += cw;
        // Draw outer rectangle
        cs.addRect(x, y, w, h);
        cs.stroke();
        // Draw vertical lines for columns
        float currentX = x;
        for (int i = 0; i < colWidths.length - 1; i++) {
            currentX += colWidths[i];
            cs.moveTo(currentX, y);
            cs.lineTo(currentX, y + h);
            cs.stroke();
        }
    }

    private static void drawText(PDPageContentStream cs, String label, String value,
                                 float x, float y, float w, float h, int fontSize) throws Exception {
        float padding = 3f;
        float lineHeight = fontSize + 2;

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, fontSize);
        cs.newLineAtOffset(x + padding, y + h - lineHeight - padding);
        cs.showText(label);
        cs.endText();

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, fontSize);
        cs.newLineAtOffset(x + padding, y + h - 2*lineHeight - padding);
        cs.showText(value);
        cs.endText();
    }

    private static void drawAddress(PDPageContentStream cs, JsonNode node, String label,
                                    float x, float y, float w, float h, int fontSize) throws Exception {
        float padding = 3f;
        float lineHeight = fontSize + 2;
        float currentY = y + h - lineHeight - padding;

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, fontSize);
        cs.newLineAtOffset(x + padding, currentY);
        cs.showText(label);
        cs.endText();
        currentY -= lineHeight;

        String[] fields;
        if(label.equalsIgnoreCase("Ship From")) {
            fields = new String[]{
                    getJsonText(node,"name"),
                    getJsonText(node,"addressLine1"),
                    getJsonText(node,"addressLine2"),
                    getJsonText(node,"supplierCode"),
                    getJsonText(node,"countryOfOrigin")
            };
        } else {
            fields = new String[]{
                    getJsonText(node,"name"),
                    getJsonText(node,"addressLine1"),
                    getJsonText(node,"addressLine2"),
                    getJsonText(node,"plant"),
                    getJsonText(node,"sLoc")
            };
        }

        for(String field: fields){
            if(!field.isEmpty()){
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, fontSize);
                cs.newLineAtOffset(x + padding, currentY);
                cs.showText(field);
                cs.endText();
                currentY -= lineHeight;
                if(currentY <= y + padding) break; // stop if overflow
            }
        }
    }

    private static void drawTextAndBarcode(PDPageContentStream cs, String label, String value,
                                           String barcodeData, PDDocument doc,
                                           float x, float y, float w, float h, int fontSize) throws Exception {
        float padding = 3f;
        float lineHeight = fontSize + 2;

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, fontSize);
        cs.newLineAtOffset(x + padding, y + h - lineHeight - padding);
        cs.showText(label);
        cs.endText();

        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, fontSize);
        cs.newLineAtOffset(x + padding, y + h - 2*lineHeight - padding);
        cs.showText(value);
        cs.endText();

        // Barcode
        float barcodeHeight = 0.5f * INCH;
        float barcodeWidth = w - 2*padding - 2*0.25f*INCH;
        BitMatrix matrix = new MultiFormatWriter().encode(barcodeData, BarcodeFormat.CODE_128,
                (int)barcodeWidth, (int)barcodeHeight);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
        PDImageXObject pdImage = PDImageXObject.createFromByteArray(doc, toByteArray(image), "barcode");

        cs.drawImage(pdImage, x + padding, y + padding, barcodeWidth, barcodeHeight);
    }

    private static void drawPoMiscBlock(PDPageContentStream cs, JsonNode root,
                                        float x, float y, float w, float h, int fontSize) throws Exception {
        float padding = 3f;
        float lineHeight = fontSize + 2;
        float currentY = y + h - padding - lineHeight;

        String[][] fields = {
                {"PO No.", getJsonText(root,"poNumber")},
                {"PO Line No.", getJsonText(root,"poLineNumber")},
                {"Lot No.", getJsonText(root,"lotNumber")},
                {"QML", getJsonText(root,"qml")},
                {"PCD#", getJsonText(root,"pcd")},
                {"PROD Date", getJsonText(root,"productionDate")},
                {"EXP Date", getJsonText(root,"expirationDate")}
        };

        for(String[] field: fields){
            if(currentY <= y + padding) break;
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, fontSize);
            cs.newLineAtOffset(x + padding, currentY);
            cs.showText(field[0]);
            cs.endText();
            currentY -= lineHeight;

            if(currentY <= y + padding) break;
            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA, fontSize);
            cs.newLineAtOffset(x + padding, currentY);
            cs.showText(field[1]);
            cs.endText();
            currentY -= lineHeight;
        }
    }

    private static void drawQRCode(PDPageContentStream cs, PDDocument doc, String data,
                                   float x, float y, float width, float height) throws Exception {
        BitMatrix matrix = new MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE,
                (int)width, (int)height);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
        PDImageXObject qrImg = PDImageXObject.createFromByteArray(doc, toByteArray(image), "qrcode");
        cs.drawImage(qrImg, x, y, width, height);
    }

    private static byte[] toByteArray(BufferedImage image) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", baos);
        byte[] bytes = baos.toByteArray();
        baos.close();
        return bytes;
    }
}
