package com.slicelending.identity.infrastructure.mail;

import com.slicelending.identity.application.event.EmailVerificationRequestedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class EmailVerificationEventListener {
    private final JavaMailSender mailSender;
    private final String verificationBaseUrl;
    private final String fromAddress;


    public EmailVerificationEventListener(JavaMailSender mailSender, @Value("${app.email-verification.base-url}") String verificationBaseUrl, @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.verificationBaseUrl = verificationBaseUrl;
        this.fromAddress = fromAddress;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(EmailVerificationRequestedEvent event){
        String verficationLink = verificationBaseUrl+ "?token=" + event.rawToken();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(event.email());
        message.setSubject("Verify your slice lending email");
        message.setText(
                "Welcome to slice Lending!\n\n" +
                        "Verify your email using this link: \n" +
                        verficationLink
        );

        mailSender.send(message);
    }
}
