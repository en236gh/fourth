package com.backend.fourth.student.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class ExamPassPdfService {

    public String toQrImageBase64(String qrPayload) {
        try {
            BufferedImage image = encodeQrImage(qrPayload, 280);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (WriterException | IOException ex) {
            throw new IllegalStateException("Failed to generate QR image", ex);
        }
    }

    public byte[] buildPdf(ExaminationPassDocumentData data) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font mutedFont = FontFactory.getFont(FontFactory.HELVETICA, 9, BaseColor.DARK_GRAY);

            Paragraph title = new Paragraph("Digital Examination Attendance System", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("Examination Pass", headingFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(4f);
            document.add(subtitle);

            Paragraph period = new Paragraph(
                    data.academicYear() + " · Semester " + data.semester(), mutedFont);
            period.setAlignment(Element.ALIGN_CENTER);
            period.setSpacingAfter(14f);
            document.add(period);

            document.add(sectionHeading("Student Details", headingFont));
            document.add(infoTable(new String[][]{
                    {"Computer Number", data.computerNumber()},
                    {"Full Name", data.fullName()},
                    {"School / Faculty", data.school()},
                    {"Programme", data.programme()},
                    {"Year of Study", String.valueOf(data.currentYear())}
            }, bodyFont));

            document.add(sectionHeading("Examinations", headingFont));
            document.add(examinationsTable(data.examinations(), smallFont));

            BufferedImage qrImage = encodeQrImage(data.qrToken(), 220);
            ByteArrayOutputStream qrBytes = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", qrBytes);
            Image qr = Image.getInstance(qrBytes.toByteArray());
            qr.setAlignment(Element.ALIGN_CENTER);
            qr.scaleAbsolute(160f, 160f);
            qr.setSpacingBefore(14f);
            document.add(qr);

            Paragraph qrCaption = new Paragraph(
                    "One secure QR code for all examinations below — present this at each venue",
                    mutedFont);
            qrCaption.setAlignment(Element.ALIGN_CENTER);
            qrCaption.setSpacingBefore(4f);
            document.add(qrCaption);

            Paragraph generated = new Paragraph(
                    "Generated: " + data.generatedAt() + "  ·  Valid until: " + data.expiresAt(),
                    mutedFont);
            generated.setAlignment(Element.ALIGN_CENTER);
            generated.setSpacingBefore(10f);
            document.add(generated);

            document.close();
            return baos.toByteArray();
        } catch (DocumentException | WriterException | IOException ex) {
            throw new IllegalStateException("Failed to generate examination pass PDF", ex);
        }
    }

    private Paragraph sectionHeading(String text, Font font) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setSpacingBefore(10f);
        paragraph.setSpacingAfter(6f);
        return paragraph;
    }

    private PdfPTable infoTable(String[][] rows, Font font) throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2f, 4f});
        for (String[] row : rows) {
            PdfPCell label = new PdfPCell(new Phrase(row[0], font));
            label.setBorder(PdfPCell.NO_BORDER);
            label.setPadding(3f);
            PdfPCell value = new PdfPCell(new Phrase(row[1] == null ? "—" : row[1], font));
            value.setBorder(PdfPCell.NO_BORDER);
            value.setPadding(3f);
            table.addCell(label);
            table.addCell(value);
        }
        return table;
    }

    private PdfPTable examinationsTable(List<ExaminationRow> examinations, Font font) throws DocumentException {
        PdfPTable table = new PdfPTable(6);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 1.4f, 1.2f, 1.6f, 1.4f, 0.8f});

        String[] headers = {"Course", "Date", "Time", "Venue", "Building", "Seat"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, font));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setPadding(4f);
            table.addCell(cell);
        }

        for (ExaminationRow exam : examinations) {
            table.addCell(cell(exam.courseCode(), font));
            table.addCell(cell(exam.examDate(), font));
            table.addCell(cell(exam.startTime() + "–" + exam.endTime(), font));
            table.addCell(cell(exam.venueName(), font));
            table.addCell(cell(exam.building(), font));
            table.addCell(cell(exam.seatNumber(), font));
        }
        return table;
    }

    private PdfPCell cell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text == null ? "—" : text, font));
        cell.setPadding(4f);
        return cell;
    }

    private BufferedImage encodeQrImage(String payload, int size) throws WriterException {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix matrix = new QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size, hints);
        return MatrixToImageWriter.toBufferedImage(matrix);
    }

    public record ExaminationRow(
            String courseCode,
            String examDate,
            String startTime,
            String endTime,
            String venueName,
            String building,
            String seatNumber
    ) {
    }

    public record ExaminationPassDocumentData(
            String computerNumber,
            String fullName,
            String school,
            String programme,
            Integer currentYear,
            String academicYear,
            Integer semester,
            List<ExaminationRow> examinations,
            String qrToken,
            String generatedAt,
            String expiresAt
    ) {
    }
}
