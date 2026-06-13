package com.app.infrastructure.pdf;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class PdfProviderUnitTest {

    private RemotePdfProvider pdfProvider;
    private RemotePdfClient client;

    @BeforeEach
    void setup() {
        client = Mockito.mock(RemotePdfClient.class);
        pdfProvider = new RemotePdfProvider();
        pdfProvider.client = client;
    }

    @Test
    void testGeneratePdf() {
        PdfRequestDTO request = new PdfRequestDTO();
        InputStream expectedStream = new ByteArrayInputStream("fake-pdf".getBytes());
        
        when(client.generate(any(PdfRequestDTO.class))).thenReturn(expectedStream);

        InputStream result = pdfProvider.generatePdf(request);

        assertNotNull(result);
        assertEquals(expectedStream, result);
    }

    @Test
    void testGeneratePdf_ExceptionFallback() throws Exception {
        PdfRequestDTO request = new PdfRequestDTO();
        
        when(client.generate(any(PdfRequestDTO.class))).thenThrow(new RuntimeException("Remote PDF service down"));

        InputStream result = pdfProvider.generatePdf(request);

        assertNotNull(result);
        byte[] bytes = result.readAllBytes();
        String resultStr = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(resultStr.contains("%PDF-1.4"));
    }
}
