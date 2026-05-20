package skinsmarket.demo.service;

import org.springframework.stereotype.Service;

@Service
public class EmailService {

    public void sendEmailVerificationEmail(String to, String verificationLink) {
        System.out.println("[MAIL] Para: " + to);
        System.out.println("[MAIL] Verificar email: " + verificationLink);
    }

    public void sendPasswordResetEmail(String to, String resetLink) {
        System.out.println("[MAIL] Para: " + to);
        System.out.println("[MAIL] Recuperar password: " + resetLink);
    }
}
