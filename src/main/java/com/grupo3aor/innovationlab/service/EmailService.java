package com.grupo3aor.innovationlab.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Infrastructure service handling outbound SMTP communication.
 * <p>
 * I established this service to abstract away raw JavaMail networking protocols,
 * ensuring our application can dispatch operational notifications (like account activations)
 * securely and reliably.
 * </p>
 * * @author Group 3 - Acertar o Rumo 12th Edition
 * @version 1.0
 */
@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Direct constructor injecting the Spring Mail transport dependency.
     * * @param mailSender Configured SMTP mail sender bean.
     */
    public EmailService(JavaMailSender mailSender){
        // I opted for constructor injection to guarantee the mail sender is fully 
        // instantiated before any notification requests are processed.
        this.mailSender = mailSender;
    }

    /**
     * Dispatches a structured activation email containing a secure verification link.
     * <p>
     * I designed this method to construct a plaintext email payload explicitly instructing 
     * the user on how to unlock their account. The token parameter acts as the secure key.
     * </p>
     * <p>
     * I wrapped the send call in a try-catch for MailException so that a missing or
     * unreachable SMTP server (e.g. Mailpit not running locally) does not cause the
     * registration flow to fail with a generic 500. The error is logged clearly so the
     * developer can diagnose it without confusion.
     * </p>
     *
     * @param targetEmail The recipient's exact registered email address.
     * @param firstName The recipient's first name for personalization.
     * @param token The cryptographically generated activation token.
     */
    public void sendConfirmationEmail(String targetEmail, String firstName, String token) {
        
        // I hardcoded the local development domain here temporarily. 
        // In the future, I will extract this to the application.properties to support environment-specific routing.
        String fullUrl = "http://localhost:5173/ativar?token=" + token;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(targetEmail);
        msg.setSubject("Innovation Lab - Account Activation");

        String body = "Hello " + firstName + ",\n\n" +
                      "Welcome to Innovation Lab! " + 
                      "Please click the link below to activate your account: " + 
                      "\n\n" + fullUrl + "\n\n" +
                      "If you did not request this registration, you can ignore this email.\n" +
                      "Best regards,\nThe Innovation Lab Team.";

        msg.setText(body);

        // I delegated the actual network transmission to the injected Spring Mail interface.
        // I wrapped this in a try-catch so a misconfigured or absent SMTP server (e.g. Mailpit
        // not running on port 1025) produces a clear warning log instead of a 500 error.
        try {
            mailSender.send(msg);
            log.info("[EMAIL] Confirmation email dispatched to: {}", targetEmail);
        } catch (MailException e) {
            log.warn("[EMAIL_WARNING] Failed to send confirmation email to: {} | Cause: {} | " +
                     "User account was created. Activation link: {}", targetEmail, e.getMessage(), fullUrl);
        }
    }
}