package com.grupo3aor.innovationlab.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    void sendConfirmationEmail_Success() {
        emailService.sendConfirmationEmail("target@example.com", "John", "token123");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);
        assertEquals("target@example.com", sentMessage.getTo()[0]);
        assertEquals("Innovation Lab - Account Activation", sentMessage.getSubject());
        assertTrue(sentMessage.getText().contains("http://localhost:5173/ativar?token=token123"));
        assertTrue(sentMessage.getText().contains("Hello John,"));
    }

    @Test
    void sendConfirmationEmail_MailExceptionShouldNotThrow() {
        doThrow(new MailException("SMTP Server Down") {}).when(mailSender).send(any(SimpleMailMessage.class));

        // It should catch the exception and just log a warning, not throw
        emailService.sendConfirmationEmail("target@example.com", "John", "token123");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendPasswordResetEmail_Success() {
        emailService.sendPasswordResetEmail("user@example.com", "Jane", "reset123");

        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);
        assertEquals("user@example.com", sentMessage.getTo()[0]);
        assertEquals("Innovation Lab - Password Reset Request", sentMessage.getSubject());
        assertTrue(sentMessage.getText().contains("http://localhost:5173/reset-password?token=reset123"));
        assertTrue(sentMessage.getText().contains("Hello Jane,"));
    }

    @Test
    void sendPasswordResetEmail_MailExceptionShouldNotThrow() {
        doThrow(new MailException("SMTP Server Down") {}).when(mailSender).send(any(SimpleMailMessage.class));

        // It should catch the exception and just log a warning, not throw
        emailService.sendPasswordResetEmail("user@example.com", "Jane", "reset123");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
