package it.homegym.util;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public final class MailUtil {

    private MailUtil() {}

    public static void send(String to, String subject, String bodyHtml) throws MessagingException {
        String host = System.getenv("SMTP_HOST");
        String port = System.getenv("SMTP_PORT");
        String user = System.getenv("SMTP_USER");
        String pass = System.getenv("SMTP_PASSWORD");
        String from = System.getenv("MAIL_FROM");
        String sslEnv = System.getenv("SMTP_SSL"); // "true" per forzare SSL

        if (host == null || port == null || from == null) {
            throw new IllegalStateException("SMTP config mancante (SMTP_HOST/SMTP_PORT/MAIL_FROM).");
        }

        boolean useAuth = (user != null && !user.isBlank());
        boolean useSsl = "true".equalsIgnoreCase(sslEnv) || "465".equals(port);

        Properties props = new Properties();
        props.put("mail.smtp.auth", String.valueOf(useAuth));
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        if (useSsl) {
            // SSL (implicit) e.g. port 465
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.trust", host);
        } else {
            // STARTTLS (explicit) e.g. port 587
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.trust", host);
        }

        Session session;
        if (useAuth) {
            Authenticator auth = new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass != null ? pass : "");
                }
            };
            session = Session.getInstance(props, auth);
        } else {
            session = Session.getInstance(props);
        }

        Message msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(from));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
        msg.setSubject(subject);
        msg.setSentDate(new java.util.Date());
        msg.setContent(bodyHtml, "text/html; charset=UTF-8");

        Transport.send(msg);
    }
}
