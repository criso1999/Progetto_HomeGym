package it.homegym.util;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailSender {

    private static Session createSession() {
        String host = System.getenv("SMTP_HOST");
        String port = System.getenv().getOrDefault("SMTP_PORT", "587");
        final String user = System.getenv("SMTP_USER");
        final String pass = System.getenv("SMTP_PASS");
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.auth", user != null && !user.isEmpty() ? "true" : "false");
        props.put("mail.smtp.starttls.enable", "true");

        if (user != null && !user.isBlank()) {
            return Session.getInstance(props, new Authenticator() {
                @Override protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });
        } else {
            return Session.getInstance(props);
        }
    }

    public static void sendVerificationEmail(String to, String subject, String htmlBody) throws MessagingException {
        Session session = createSession();
        String from = System.getenv().getOrDefault("SMTP_FROM", "no-reply@homegym.local");
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(from));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
        msg.setSubject(subject, "UTF-8");
        msg.setContent(htmlBody, "text/html; charset=UTF-8");
        Transport.send(msg);
    }
}
