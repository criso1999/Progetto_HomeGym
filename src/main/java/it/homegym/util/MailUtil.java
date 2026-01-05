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

        if (host == null || port == null || from == null) {
            throw new IllegalStateException("SMTP config mancante (SMTP_HOST/SMTP_PORT/MAIL_FROM).");
        }

        Properties props = new Properties();
        props.put("mail.smtp.auth", (user != null && !user.isEmpty()) ? "true" : "false");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        Session session;
        if (user != null && !user.isEmpty()) {
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
