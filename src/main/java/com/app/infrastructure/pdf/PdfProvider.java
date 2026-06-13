package com.app.infrastructure.pdf;

import java.io.InputStream;

/**
 * Interface for PDF generation service.
 */
public interface PdfProvider {
    /**
     * Generates a PDF from HTML and returns an InputStream for streaming.
     * 
     * @param request The PDF request data
     * @return InputStream containing the PDF bytes
     */
    InputStream generatePdf(PdfRequestDTO request);
}
