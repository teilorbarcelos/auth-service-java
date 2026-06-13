package com.app.infrastructure.email;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class EmailProviderTest {

    private EmailProvider emailProvider;
    private Mailer mailer;

    @BeforeEach
    void setup() {
        mailer = mock(Mailer.class);
        emailProvider = new EmailProvider();
        emailProvider.mailer = mailer;
    }

    @Test
    void testSendEmail_Success() {
        emailProvider.sendEmail("test@test.com", "Subject", "<h1>Html</h1>");
        verify(mailer).send(any(Mail.class));
    }

    @Test
    void testSendEmail_Error() {
        doThrow(new RuntimeException("Mail server down")).when(mailer).send(any(Mail.class));
        // Should not throw exception, just log it
        assertDoesNotThrow(() -> emailProvider.sendEmail("test@test.com", "Subject", "<h1>Html</h1>"));
        verify(mailer).send(any(Mail.class));
    }

    @Test
    void testEmailTemplates_Render() {
        String rendered = EmailTemplates.render(EmailTemplates.WELCOME_TEMPLATE, Map.of("name", "John"));
        assertTrue(rendered.contains("John"));
        
        String rendered2 = EmailTemplates.render("Hello {{name}}", Map.of("name", "World"));
        assertTrue(rendered2.contains("World"));

        // Test with null value in context
        java.util.Map<String, String> context = new java.util.HashMap<>();
        context.put("name", null);
        String rendered3 = EmailTemplates.render("Hello {{name}}", context);
        assertTrue(rendered3.contains("Hello "));

        // Call private constructor for 100% coverage
        assertDoesNotThrow(() -> {
            java.lang.reflect.Constructor<EmailTemplates> constructor = EmailTemplates.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            constructor.newInstance();
        });
    }
}
