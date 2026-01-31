package it.homegym.util;

import javax.mail.*;
import javax.mail.internet.*;
import java.util.Properties;

public class EmailSender {

    private static final String SMTP_HOST = System.getenv("SMTP_HOST");
    private static final String SMTP_PORT = System.getenv().getOrDefault("SMTP_PORT", "587");
    private static final String SMTP_USER = System.getenv().getOrDefault("SMTP_USER", "");
    private static final String SMTP_PASS = System.getenv().getOrDefault("SMTP_PASSWORD", "");
    private static final String MAIL_FROM  = System.getenv().getOrDefault("MAIL_FROM", "no-reply@homegym.local");
    private static final boolean SSL_ENABLED = Boolean.parseBoolean(System.getenv().getOrDefault("SMTP_SSL", "false"));
    private static final boolean MAIL_DEBUG = "1".equals(System.getenv().getOrDefault("MAIL_DEBUG","0"));

    private static final Session session;

    static {
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.auth", (SMTP_USER != null && !SMTP_USER.isBlank()) ? "true" : "false");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

        if (SSL_ENABLED) {
            // SSL (ex: port 465)
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.port", SMTP_PORT);
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.ssl.trust", SMTP_HOST);
        } else {
            // STARTTLS (ex: port 587)
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.ssl.trust", SMTP_HOST);
        }

        if (SMTP_USER != null && !SMTP_USER.isBlank()) {
            final String user = SMTP_USER;
            final String pass = SMTP_PASS;
            session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(user, pass);
                }
            });
        } else {
            session = Session.getInstance(props);
        }

        session.setDebug(MAIL_DEBUG);
    }

    public static void sendHtmlEmail(String to, String subject, String html) throws MessagingException {
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(new InternetAddress(MAIL_FROM));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to, false));
        msg.setSubject(subject, "UTF-8");

        MimeBodyPart body = new MimeBodyPart();
        body.setContent(html, "text/html; charset=UTF-8");

        Multipart mp = new MimeMultipart();
        mp.addBodyPart(body);
        msg.setContent(mp);
        msg.setHeader("X-Mailer", "HomeGym-Mailer");

        Transport.send(msg);
    }

    // helper convenience
    public static void sendVerificationEmail(String to, String subject, String html) throws MessagingException {
        sendHtmlEmail(to, subject, html);
    }
}
