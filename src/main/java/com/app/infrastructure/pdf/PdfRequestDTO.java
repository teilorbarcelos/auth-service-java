package com.app.infrastructure.pdf;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.Map;

/**
 * DTO for PDF generation request.
 */
@RegisterForReflection
public class PdfRequestDTO {

    private String template;
    private Map<String, Object> data;
    private Options options;

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }

    @RegisterForReflection
    public static class Options {
        private boolean landscape = false;
        private String format = "A4";

        public boolean isLandscape() {
            return landscape;
        }

        public void setLandscape(boolean landscape) {
            this.landscape = landscape;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }
    }
}
