
package com.financepilot.backend.statement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.financepilot.backend.statement.controller.StatementController;
import com.financepilot.backend.statement.dto.StatementTextResponse;
import com.financepilot.backend.statement.service.PdfExtractionService;
import com.financepilot.backend.statement.exception.StatementExceptionHandler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StatementController.class)
@AutoConfigureMockMvc
class StatementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PdfExtractionService pdfExtractionService;

    @Test
    void shouldUploadPdfSuccessfully() throws Exception {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "extrato.pdf",
                MediaType.APPLICATION_PDF_VALUE,
                "fake pdf content".getBytes()
        );

        StatementTextResponse response =
                new StatementTextResponse(
                        "extrato.pdf",
                        1,
                        20,
                        "Sample statement text"
                );

        when(pdfExtractionService.extract(any()))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/statements/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName")
                        .value("extrato.pdf"))
                .andExpect(jsonPath("$.pages")
                        .value(1));
    }
}
