# Email Service Implementation

This document describes the implementation of OTP email functionality for the Spring Boot e-commerce application.

## Overview

The application now includes a complete email service for sending OTP (One-Time Password) codes for password reset functionality. The implementation uses Spring Boot's JavaMailSender for SMTP-based email delivery.

## Components Implemented

### 1. Dependencies (pom.xml)
- Added `spring-boot-starter-mail` dependency for email functionality

### 2. Configuration (application.properties)
```properties
# Email Configuration (SMTP)
spring.mail.host=${MAIL_HOST:smtp.gmail.com}
spring.mail.port=${MAIL_PORT:587}
spring.mail.username=${MAIL_USERNAME:your-email@gmail.com}
spring.mail.password=${MAIL_PASSWORD:your-app-password}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=5000
spring.mail.properties.mail.smtp.timeout=5000
spring.mail.properties.mail.smtp.writetimeout=5000

# Email settings
app.mail.from=${MAIL_FROM:noreply@shop.local}
app.mail.from-name=${MAIL_FROM_NAME:Shop Support}
```

### 3. Email Service Interface
**File**: `src/main/java/com/jackson/demo/service/EmailService.java`
- Defines contract for email operations
- Methods: `sendOtpEmail()`, `sendSimpleEmail()`

### 4. Email Service Implementation
**File**: `src/main/java/com/jackson/demo/service/impl/EmailServiceImpl.java`
- Implements email sending functionality using JavaMailSender
- Sends HTML-formatted OTP emails with professional styling
- Includes security warnings and expiration information
- Comprehensive error handling and logging

### 5. Updated AuthService
**File**: `src/main/java/com/jackson/demo/service/AuthService.java`
- Injected EmailService dependency
- Enabled OTP email sending in `forgotPassword()` method
- Removed TODO comment and implemented actual email functionality

## Email Provider Information

### Current Configuration
- **Provider**: Gmail SMTP (default)
- **Host**: `smtp.gmail.com`
- **Port**: `587` (TLS)
- **Security**: STARTTLS encryption

### Free vs Paid Service
- **Gmail SMTP**: Free for personal use with Gmail accounts
- **Limitations**: 500 emails per day for free Gmail accounts
- **Alternative Providers**: Can be easily configured for:
  - SendGrid (free tier: 100 emails/day)
  - Mailgun (free tier: 5,000 emails/month)
  - AWS SES (pay-as-you-go)
  - Other SMTP providers

## Email Sender Information

### Default Configuration
- **From Address**: `noreply@shop.local`
- **From Name**: `Shop Support`
- **Customizable**: Via environment variables `MAIL_FROM` and `MAIL_FROM_NAME`

### Email Content
- **Subject**: "Password Reset"
- **Format**: HTML with responsive design
- **Content**: Includes OTP code, expiration time, and security warnings
- **Expiration**: 10 minutes (configurable via `app.otp.expiration-minutes`)

## Configuration Options

### Environment Variables
```bash
# SMTP Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password

# Email Settings
MAIL_FROM=noreply@yoursite.com
MAIL_FROM_NAME=Your Site Support
MAIL_FROM_NAME=Your Site Support

# OTP Settings
app.otp.expiration-minutes=10
```

### For Gmail Users
1. Enable 2-Factor Authentication
2. Generate an App Password:
   - Go to Google Account settings
   - Security → 2-Step Verification → App passwords
   - Generate password for "Mail"
3. Use the app password in `MAIL_PASSWORD`

### For Other Providers
Update the SMTP settings in `application.properties`:
```properties
spring.mail.host=smtp.sendgrid.net
spring.mail.port=587
spring.mail.username=apikey
spring.mail.password=your-sendgrid-api-key
```

## Usage

### Password Reset Flow
1. User requests password reset via `/api/v1/auth/forgot-password`
2. System generates 6-digit OTP code
3. OTP is stored in database with expiration time
4. Email is sent to user with OTP code
5. User enters OTP to verify identity
6. User can then reset their password

### API Endpoints
- `POST /api/v1/auth/forgot-password` - Request OTP
- `POST /api/v1/auth/verify-otp` - Verify OTP
- `POST /api/v1/auth/reset-password` - Reset password

## Security Features

1. **OTP Expiration**: Codes expire after 10 minutes
2. **Single Use**: OTP codes can only be used once
3. **Security Warnings**: Emails include warnings not to share OTP codes
4. **Logging**: All email operations are logged for monitoring
5. **Error Handling**: Graceful handling of email delivery failures

## Testing

The email service can be tested by:
1. Starting the application with valid SMTP credentials
2. Calling the forgot password endpoint
3. Checking the recipient's email for the OTP code
4. Verifying the OTP and resetting the password

## Future Enhancements

1. **Email Templates**: Create separate HTML templates for different email types
2. **Email Queue**: Implement async email sending for better performance
3. **Multiple Providers**: Support for multiple email providers with fallback
4. **Email Tracking**: Track email delivery and open rates
5. **Rate Limiting**: Prevent email spam by limiting OTP requests
6. **Internationalization**: Support for multiple languages in email content

## Testing the Implementation

The email service has been successfully implemented and compiled without errors. To test:

1. **Start the application** with valid SMTP credentials
2. **Call the forgot password endpoint**: `POST /api/v1/auth/forgot-password`
3. **Check the recipient's email** for the OTP code
4. **Verify the OTP** and reset the password

The implementation includes comprehensive error handling and logging for production use.
