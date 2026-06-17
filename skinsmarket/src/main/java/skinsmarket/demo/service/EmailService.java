package skinsmarket.demo.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final boolean mailEnabled;
    private final String mailFrom;

    public EmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${application.mail.enabled:false}") boolean mailEnabled,
            @Value("${application.mail.from:}") String mailFrom) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.mailEnabled = mailEnabled;
        this.mailFrom = mailFrom;
    }

    public void sendEmailVerificationEmail(String to, String verificationLink) {
        sendOrLog(
                to,
                "Verifica tu cuenta en SkinsMarket",
                """
                Hola,

                Para activar tu cuenta en SkinsMarket, abri este link:

                %s

                Si no creaste esta cuenta, ignora este mail.
                """.formatted(verificationLink),
                "Verificar email: " + verificationLink);
    }

    public void sendPasswordResetEmail(String to, String resetLink) {
        sendOrLog(
                to,
                "Recuperar contraseña de SkinsMarket",
                """
                Hola,

                Para cambiar tu contraseña, abri este link:

                %s

                El link vence pronto. Si no pediste este cambio, ignora este mail.
                """.formatted(resetLink),
                "Recuperar password: " + resetLink);
    }

    private void sendOrLog(String to, String subject, String body, String fallbackLine) {
        if (!mailEnabled || mailSender == null || mailFrom == null || mailFrom.isBlank()) {
            logFallback(to, fallbackLine);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            System.out.println("[MAIL] Email enviado a: " + to + " | " + subject);
        } catch (MailException e) {
            System.err.println("[MAIL] No se pudo enviar email real a " + to + ": " + e.getMessage());
            logFallback(to, fallbackLine);
        }
    }

    private void logFallback(String to, String line) {
        System.out.println("[MAIL] Para: " + to);
        System.out.println("[MAIL] " + line);
    }
}
