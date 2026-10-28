package com.financepilot.backend.statement.controller;

import com.financepilot.backend.statement.dto.StatementTextResponse;
import com.financepilot.backend.statement.service.PdfExtractionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/statements")
public class StatementController {

    private final PdfExtractionService pdfExtractionService;

    public StatementController(PdfExtractionService pdfExtractionService) {
        this.pdfExtractionService = pdfExtractionService;
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<StatementTextResponse> upload(
            @RequestParam("file") MultipartFile file) {

        StatementTextResponse response =
                pdfExtractionService.extract(file);

        return ResponseEntity.ok(response);
    }
}