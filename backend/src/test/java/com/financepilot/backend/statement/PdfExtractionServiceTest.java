
package com.financepilot.backend.statement;

import com.financepilot.backend.statement.dto.StatementTextResponse;
import com.financepilot.backend.statement.exception.InvalidPdfException;
import com.financepilot.backend.statement.service.PdfExtractionService;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PdfExtractionServiceTest {

    private final PdfExtractionService service =
            new PdfExtractionService();

    @Test
    void shouldExtractTextFromValidPdf() throws IOException {

        byte[] pdfBytes = createTestPdf();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "statement.pdf",
                "application/pdf",
                pdfBytes
        );

        StatementTextResponse response = service.extract(file);

        assertEquals("statement.pdf", response.fileName());
        assertEquals(1, response.pages());
        assertTrue(response.text().contains("SUPERMERCADO"));
        assertTrue(response.text().contains("185,90"));
        assertEquals(response.text().length(), response.characters());
    }

    @Test
    void shouldRejectEmptyFile() {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        assertThrows(
                InvalidPdfException.class,
                () -> service.extract(file)
        );
    }

    @Test
    void shouldRejectFakePdf() {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.pdf",
                "application/pdf",
                "This is not a PDF".getBytes(StandardCharsets.UTF_8)
        );

        assertThrows(
                InvalidPdfException.class,
                () -> service.extract(file)
        );
    }

    private byte[] createTestPdf() throws IOException {

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream content =
                         new PDPageContentStream(document, page)) {

                content.beginText();
                content.setFont(
                        new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                        12
                );
                content.newLineAtOffset(50, 700);
                content.showText("06/10 SUPERMERCADO -185,90");
                content.endText();
            }

            document.save(output);
            return output.toByteArray();
        }
    }
}
