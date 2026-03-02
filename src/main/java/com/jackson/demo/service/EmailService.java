package com.jackson.demo.service;

/**
 * Service interface for sending emails.
 * Provides methods for sending OTP emails and other email notifications.
 */
public interface EmailService {

    /**
     * Send an OTP email to the specified recipient.
     *
     * @param toEmail the recipient's email address
     * @param otpCode the OTP code to include in the email
     * @param subject the subject of the email
     */
    void sendOtpEmail(String toEmail, String otpCode, String subject);

    /**
     * Send a simple text email.
     *
     * @param toEmail the recipient's email address
     * @param subject the subject of the email
     * @param body the body content of the email
     */
    void sendSimpleEmail(String toEmail, String subject, String body);
}