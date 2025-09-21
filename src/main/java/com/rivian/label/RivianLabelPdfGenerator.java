package com.rivian.label;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RivianLabelPdfGenerator {

    public static void main(String[] args) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new File("src/input.json")).get("container_label");

            // Generate QR and barcodes
            String qrPath = generateQRCode(root.get("qr_code").get("encoded_string").asText(), "qr.png", 216, 216);
            String pnBarcodePath = generateBarcode(root.get("partNumber").asText(), "pn.png", 324, 36);
            String qtyBarcodePath = generateBarcode(root.get("quantity").asText(), "qty.png", 180, 36);
            String lpnBarcodePath = generateBarcode(root.get("lpn_1j").asText(), "lpn.png", 324, 36);

            // Read HTML template
            String htmlTemplate = new String(Files.readAllBytes(Paths.get("template.html")));

            // Replace placeholders
            htmlTemplate = htmlTemplate
                    .replace("${shipFrom.name}", getText(root, "ship_from", "name"))
                    .replace("${shipFrom.addressLine1}", getText(root, "ship_from", "addressLine1"))
                    .replace("${shipFrom.addressLine2}", getText(root, "ship_from", "addressLine2"))
                    .replace("${shipFrom.supplierCode}", getText(root, "ship_from", "supplierCode"))
                    .replace("${shipFrom.countryOfOrigin}", getText(root, "ship_from", "countryOfOrigin"))

                    .replace("${shipTo.name}", getText(root, "ship_to", "name"))
                    .replace("${shipTo.addressLine1}", getText(root, "ship_to", "addressLine1"))
                    .replace("${shipTo.addressLine2}", getText(root, "ship_to", "addressLine2"))
                    .replace("${shipTo.plant}", getText(root, "ship_to", "plant"))
                    .replace("${shipTo.sLoc}", getText(root, "ship_to", "sLoc"))

                    .replace("${partNumber}", getText(root, "partNumber"))
                    .replace("${quantity}", getText(root, "quantity"))
                    .replace("${description}", getText(root, "partDescription"))
                    .replace("${poNumber}", getText(root, "poNumber"))
                    .replace("${poLineNumber}", getText(root, "poLineNumber"))
                    .replace("${lotNumber}", getText(root, "lotNumber"))
                    .replace("${productionDate}", getText(root, "productionDate"))
                    .replace("${expirationDate}", getText(root, "expirationDate"))
                    .replace("${qml}", getText(root, "qml"))
                    .replace("${pcd}", getText(root, "pcd"))
                    .replace("${lpn}", getText(root, "lpn_1j"))
                    .replace("${supplierInternal}", getText(root, "serialNumber"))

                    // Fix paths for OpenHTMLToPDF
                    .replace("${qrCodePath}", new File(qrPath).toURI().toString())
                    .replace("${partNumberBarcodePath}", new File(pnBarcodePath).toURI().toString())
                    .replace("${quantityBarcodePath}", new File(qtyBarcodePath).toURI().toString())
                    .replace("${lpnBarcodePath}", new File(lpnBarcodePath).toURI().toString());

            // Generate PDF
            try (OutputStream os = new FileOutputStream("rivian_label.pdf")) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.withHtmlContent(htmlTemplate, new File(".").toURI().toString());
                builder.toStream(os);
                builder.run();
            }

            System.out.println("PDF generated successfully!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String generateQRCode(String data, String filename, int width, int height) throws Exception {
        if(data == null || data.isEmpty()) throw new IllegalArgumentException("QR code data is empty");
        BitMatrix matrix = new MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, width, height);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
        File file = new File("src/" + filename);
        ImageIO.write(image, "png", file);
        return file.getAbsolutePath();
    }

    private static String generateBarcode(String data, String filename, int width, int height) throws Exception {
        if(data == null || data.isEmpty()) throw new IllegalArgumentException("Barcode data is empty");
        BitMatrix matrix = new MultiFormatWriter().encode(data, BarcodeFormat.CODE_128, width, height);
        BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
        File file = new File("src/" + filename);
        ImageIO.write(image, "png", file);
        return file.getAbsolutePath();
    }

    private static String getText(JsonNode root, String field) {
        return root.has(field) && !root.get(field).isNull() ? root.get(field).asText() : "";
    }

    private static String getText(JsonNode root, String parent, String field) {
        if(root.has(parent) && root.get(parent).has(field) && !root.get(parent).get(field).isNull()){
            return root.get(parent).get(field).asText();
        }
        return "";
    }
}
