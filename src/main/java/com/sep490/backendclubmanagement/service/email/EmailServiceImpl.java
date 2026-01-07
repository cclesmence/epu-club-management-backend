package com.sep490.backendclubmanagement.service.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

/**
 * Implementation of EmailService using JavaMailSender and Thymeleaf
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Override
    public void sendEmail(String to, String subject, String text) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true); // true = HTML content

            mailSender.send(message);
            log.info("Email sent successfully to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        } catch (MailException e) {
            log.error("Mail exception when sending to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        sendEmail(to, subject, htmlContent);
    }

    @Override
    public void sendEmailToMultiple(String[] recipients, String subject, String text) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(recipients);
            helper.setSubject(subject);
            helper.setText(text, true);

            mailSender.send(message);
            log.info("Email sent successfully to {} recipients", recipients.length);

        } catch (MessagingException e) {
            log.error("Failed to send email to multiple recipients: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        } catch (MailException e) {
            log.error("Mail exception when sending to multiple recipients: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    public void sendTemplatedEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            // Create Thymeleaf context and set variables
            Context context = new Context();
            context.setVariables(variables);

            // Process the template
            String htmlContent = templateEngine.process(templateName, context);

            // Send the email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); // true = HTML

            mailSender.send(message);
            log.info("Templated email sent successfully to: {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send templated email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send templated email", e);
        } catch (MailException e) {
            log.error("Mail exception when sending templated email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send templated email", e);
        } catch (Exception e) {
            log.error("Unexpected error when sending templated email to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("Failed to send templated email", e);
        }
    }
}

