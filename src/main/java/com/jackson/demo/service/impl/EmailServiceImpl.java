package com.jackson.demo.service.impl;

import com.jackson.demo.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Implementation of EmailService using Spring's JavaMailSender.
 * Handles sending OTP emails and other email notifications.
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendOtpEmail(String toEmail, String otpCode, String subject) {
        String htmlContent = buildOtpEmailContent(otpCode, subject);
        sendHtmlEmail(toEmail, subject, htmlContent);
    }

    @Override
    public void sendSimpleEmail(String toEmail, String subject, String body) {
        sendTextEmail(toEmail, subject, body);
    }

    /**
     * Send an HTML email with the specified content.
     *
     * @param toEmail the recipient's email address
     * @param subject the subject of the email
     * @param htmlContent the HTML content of the email
     */
    private void sendHtmlEmail(String toEmail, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            logger.info("OTP email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            logger.error("Failed to send OTP email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        } catch (UnsupportedEncodingException e) {
            logger.error("Failed to send OTP email to: {} - encoding error", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Send a plain text email.
     *
     * @param toEmail the recipient's email address
     * @param subject the subject of the email
     * @param textContent the text content of the email
     */
    private void sendTextEmail(String toEmail, String subject, String textContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message);

            helper.setFrom(fromEmail, fromName);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(textContent);

            mailSender.send(message);
            logger.info("Email sent successfully to: {}", toEmail);

        } catch (MessagingException e) {
            logger.error("Failed to send email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        } catch (UnsupportedEncodingException e) {
            logger.error("Failed to send email to: {} - encoding error", toEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    /**
     * Build HTML content for OTP email.
     *
     * @param otpCode the OTP code
     * @param subject the email subject
     * @return HTML content as string
     */
    private String buildOtpEmailContent(String otpCode, String subject) {
        int currentYear = java.time.Year.now().getValue();
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>%s</title>
                <style>
                    body {
                        font-family: Arial, sans-serif;
                        line-height: 1.6;
                        color: #333;
                        max-width: 600px;
                        margin: 0 auto;
                        padding: 20px;
                    }
                    .header {
                        background-color: #f8f9fa;
                        padding: 20px;
                        border-radius: 8px;
                        margin-bottom: 20px;
                    }
                    .otp-container {
                        background-color: #e9ecef;
                        padding: 30px;
                        border-radius: 8px;
                        text-align: center;
                        margin: 20px 0;
                    }
                    .otp-code {
                        font-size: 32px;
                        font-weight: bold;
                        letter-spacing: 5px;
                        color: #007bff;
                        margin: 10px 0;
                    }
                    .footer {
                        margin-top: 30px;
                        font-size: 12px;
                        color: #666;
                        text-align: center;
                    }
                    .warning {
                        background-color: #fff3cd;
                        border: 1px solid #ffeaa7;
                        padding: 15px;
                        border-radius: 4px;
                        margin: 20px 0;
                        font-size: 14px;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <h2>%s</h2>
                    <p>Use the code below to complete your request.</p>
                </div>

                <div class="otp-container">
                    <h3>Your OTP Code</h3>
                    <div class="otp-code">%s</div>
                    <p><strong>This code will expire in 10 minutes.</strong></p>
                </div>

                <div class="warning">
                    <strong>Security Notice:</strong><br>
                    Never share this OTP code with anyone. Our support team will never ask for this code.
                </div>

                <div class="footer">
                    <p>This is an automated message, please do not reply to this email.</p>
                    <p>&copy; %d Shop Support</p>
                </div>
            </body>
            </html>
            """.formatted(subject, subject, otpCode, currentYear);
    }
}