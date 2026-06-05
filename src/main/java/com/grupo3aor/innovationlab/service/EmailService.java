package com.grupo3aor.innovationlab.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Autowired
    public EmailService(JavaMailSender mailSender){
        this.mailSender = mailSender;
    }

    public void enviarEmailConfirmacao(String emailDestino, String nomeUtilizador, String token) {
        
        String urlCompleto = "http://localhost:5173/ativar?token=" + token;

        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(emailDestino);
        msg.setSubject("Innovation Lab - Account Activation");

        String corpo = "Hello " + nomeUtilizador + ",\n\n" +
                      "Welcome to Innovation Lab! " + 
                      "Please click the link below to activate your account: " + 
                      "\n\n" + urlCompleto + "\n\n" +
                      "If you did not request this registration, you can ignore this email.\n" +
                      "Best regards,\nThe Innovation Lab Team.";

        msg.setText(corpo);

        mailSender.send(msg);
    }
}