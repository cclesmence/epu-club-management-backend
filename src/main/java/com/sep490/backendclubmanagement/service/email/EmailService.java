package com.sep490.backendclubmanagement.service.email;

import java.util.Map;

/**
 * Service interface for sending emails
 */
public interface EmailService {

    /**
     * Send a simple text email
     *
     * @param to Recipient email address
     * @param subject Email subject
     * @param text Email body (plain text)
     */
    void sendEmail(String to, String subject, String text);

    /**
     * Send an HTML email
     *
     * @param to Recipient email address
     * @param subject Email subject
     * @param htmlContent Email body (HTML)
     */
    void sendHtmlEmail(String to, String subject, String htmlContent);

    /**
     * Send email to multiple recipients
     *
     * @param recipients List of recipient email addresses
     * @param subject Email subject
     * @param text Email body
     */
    void sendEmailToMultiple(String[] recipients, String subject, String text);

    /**
     * Send email using Thymeleaf template
     *
     * @param to Recipient email address
     * @param subject Email subject
     * @param templateName Thymeleaf template name (without .html extension)
     * @param variables Variables to pass to the template
     */
    void sendTemplatedEmail(String to, String subject, String templateName, Map<String, Object> variables);
}

