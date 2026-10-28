package com.financepilot.backend.statement.service;

import com.financepilot.backend.statement.dto.StatementTextResponse;
import com.financepilot.backend.statement.exception.InvalidPdfException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

@Service
public class PdfExtractionService {

    public StatementTextResponse extract(MultipartFile file) {

        validate(file);

        try {
            byte[] bytes = file.getBytes();

            if (bytes.length < 5
                    || bytes[0] != '%'
                    || bytes[1] != 'P'
                    || bytes[2] != 'D'
                    || bytes[3] != 'F'
                    || bytes[4] != '-') {
                throw new InvalidPdfException("O arquivo não é um PDF válido.");
            }

            try (PDDocument document = Loader.loadPDF(bytes)) {

                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);

                String text = stripper.getText(document);

                if (text.isBlank()) {
                    throw new InvalidPdfException(
                            "Não foi possível extrair texto do PDF."
                    );
                }

                return new StatementTextResponse(
                        file.getOriginalFilename(),
                        document.getNumberOfPages(),
                        text.length(),
                        text
                );
            }

        } catch (InvalidPasswordException e) {
            throw new InvalidPdfException(
                    "O PDF está protegido por senha.", e
            );

        } catch (IOException e) {
            throw new InvalidPdfException(
                    "Não foi possível ler o arquivo PDF.", e
            );
        }
    }

    private void validate(MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new InvalidPdfException("O arquivo está vazio.");
        }

        String name = file.getOriginalFilename();

        if (name == null
                || !name.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new InvalidPdfException(
                    "Envie um arquivo com extensão PDF."
            );
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new InvalidPdfException(
                    "O arquivo ultrapassa o limite de 10 MB."
            );
        }
    }
}