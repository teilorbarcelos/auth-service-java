package com.app.infrastructure.email;

import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * Email sending service.
 * Equivalent to EmailProvider.php
 */
@ApplicationScoped
public class EmailProvider {

    private static final Logger LOG = Logger.getLogger(EmailProvider.class);

    @Inject
    Mailer mailer;

    public void sendEmail(String to, String subject, String html) {
        try {
            mailer.send(Mail.withHtml(to, subject, html));
        } catch (Exception e) {
            LOG.errorv("Error sending email: {0}", e.getMessage());
        }
    }
}
