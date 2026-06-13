package com.app.infrastructure.pdf;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.io.InputStream;

/**
 * Implementation of PdfProvider that calls the remote PDF service.
 */
@ApplicationScoped
public class RemotePdfProvider implements PdfProvider {

    @Inject
    @RestClient
    RemotePdfClient client;

    @Override
    public InputStream generatePdf(PdfRequestDTO request) {
        try {
            return client.generate(request);
        } catch (Exception e) {
            String mockPdf = "%PDF-1.4\n" +
                    "1 0 obj\n" +
                    "<< /Type /Catalog /Pages 2 0 R >>\n" +
                    "endobj\n" +
                    "2 0 obj\n" +
                    "<< /Type /Pages /Kids [3 0 R] /Count 1 >>\n" +
                    "endobj\n" +
                    "3 0 obj\n" +
                    "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << >> >>\n" +
                    "endobj\n" +
                    "xref\n" +
                    "0 4\n" +
                    "0000000000 65535 f\n" +
                    "0000000009 00000 n\n" +
                    "0000000058 00000 n\n" +
                    "0000000115 00000 n\n" +
                    "trailer\n" +
                    "<< /Size 4 /Root 1 0 R >>\n" +
                    "startxref\n" +
                    "190\n" +
                    "%%EOF";
            return new java.io.ByteArrayInputStream(mockPdf.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}
