package org.example;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;
import java.util.Date;
import java.io.IOException;

/**
 * A utility class for replying to emails using Jakarta Mail.
 */
public class EmailReply {

    /**
     * Replies to a specific email message.
     *
     * @param host SMTP server host (e.g., "smtp.gmail.com")
     * @param port SMTP server port (e.g., "587" for TLS)
     * @param username Email account username
     * @param password Email account password
     * @param from Sender's email address
     * @param originalMessage The original message to reply to
     * @param replyBody The content of the reply
     * @throws MessagingException If there's an error sending the reply
     */
    public static void replyToMessage(String host, String port, 
                                     String username, String password,
                                     String from, Message originalMessage, 
                                     String replyBody) throws MessagingException {

        // Set mail server properties
        Properties properties = new Properties();
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", port);
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");

        // Create a mail session with authenticator
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        // Create a reply message
        Message replyMessage = new MimeMessage(session);

        // Set the reply headers
        Address[] replyTo = originalMessage.getReplyTo();
        if (replyTo == null || replyTo.length == 0) {
            replyTo = originalMessage.getFrom();
        }

        replyMessage.setFrom(new InternetAddress(from));
        replyMessage.setRecipients(Message.RecipientType.TO, replyTo);

        // Set the subject with "Re:" prefix if not already present
        String subject = originalMessage.getSubject();
        if (!subject.toLowerCase().startsWith("re:")) {
            subject = "Re: " + subject;
        }
        replyMessage.setSubject(subject);

        // Set In-Reply-To and References headers for proper threading
        String messageId = ((MimeMessage) originalMessage).getMessageID();
        if (messageId != null) {
            ((MimeMessage) replyMessage).setHeader("In-Reply-To", messageId);

            // Get existing References header and append the new Message-ID
            String references = ((MimeMessage) originalMessage).getHeader("References", " ");
            if (references == null) {
                references = messageId;
            } else {
                references = references + " " + messageId;
            }
            ((MimeMessage) replyMessage).setHeader("References", references);
        }

        // Set the sent date
        replyMessage.setSentDate(new Date());

        // Create the message body
        StringBuilder fullReplyBody = new StringBuilder();
        fullReplyBody.append(replyBody);
        fullReplyBody.append("\n\n----- Original Message -----\n");

        // Add original message details
        try {
            fullReplyBody.append("From: ").append(originalMessage.getFrom()[0]).append("\n");
            fullReplyBody.append("Date: ").append(originalMessage.getSentDate()).append("\n");
            fullReplyBody.append("Subject: ").append(originalMessage.getSubject()).append("\n\n");

            // Add original message content
            fullReplyBody.append(EmailReader.getMessageContent(originalMessage));
        } catch (IOException e) {
            fullReplyBody.append("Error retrieving original message content: ").append(e.getMessage());
        }

        // Set the message content
        replyMessage.setText(fullReplyBody.toString());

        // Send the message
        Transport.send(replyMessage);

        System.out.println("Reply sent successfully!");
    }

    /**
     * Replies to the latest email in the specified folder.
     *
     * @param imapHost IMAP server host (e.g., "imap.gmail.com")
     * @param imapPort IMAP server port (e.g., "993" for SSL)
     * @param smtpHost SMTP server host (e.g., "smtp.gmail.com")
     * @param smtpPort SMTP server port (e.g., "587" for TLS)
     * @param username Email account username
     * @param password Email account password
     * @param from Sender's email address
     * @param folderName Folder to read from (e.g., "INBOX")
     * @param replyBody The content of the reply
     * @throws MessagingException If there's an error reading or replying to the email
     */
    public static void replyToLatestEmail(String imapHost, String imapPort,
                                         String smtpHost, String smtpPort,
                                         String username, String password,
                                         String from, String folderName,
                                         String replyBody) throws MessagingException {

        // Get the latest email
        Message latestEmail = EmailReader.getLatestEmail(imapHost, imapPort, username, password, folderName);

        if (latestEmail == null) {
            System.out.println("No email found to reply to.");
            return;
        }

        // Reply to the email
        replyToMessage(smtpHost, smtpPort, username, password, from, latestEmail, replyBody);
    }

    /**
     * Example usage of the EmailReply class.
     */
    public static void main(String[] args) {
        // Example server details
        String imapHost = "imap.gmail.com";
        String imapPort = "993"; // SSL port
        String smtpHost = "smtp.gmail.com";
        String smtpPort = "587"; // TLS port
        String username = "quangbinh1001@gmail.com";
        String password = "<APP_PWD>"; // Use app password for Gmail
        String from = "quangbinh1001@gmail.com";
        String folderName = "INBOX";

        // Reply content
        String replyBody = "This is an automated reply to your email.\n\nThank you for your message.";

        try {
            // Reply to the latest email
            System.out.println("Replying to the latest email...");
            replyToLatestEmail(imapHost, imapPort, smtpHost, smtpPort, 
                              username, password, from, folderName, replyBody);

        } catch (MessagingException e) {
            System.out.println("Failed to reply to email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
