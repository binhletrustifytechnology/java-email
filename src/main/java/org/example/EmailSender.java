package org.example;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;
import java.io.File;
import java.util.Date;

/**
 * A utility class for sending emails using Jakarta Mail.
 */
public class EmailSender {

    /**
     * Sends a simple text email.
     *
     * @param host SMTP server host (e.g., "smtp.gmail.com")
     * @param port SMTP server port (e.g., "587" for TLS)
     * @param username Email account username
     * @param password Email account password
     * @param from Sender's email address
     * @param to Recipient's email address
     * @param subject Email subject
     * @param body Email body content
     * @throws MessagingException If there's an error sending the email
     */
    public static void sendSimpleEmail(String host, String port, 
                                      String username, String password,
                                      String from, String to, 
                                      String subject, String body) throws MessagingException {
        
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
        
        // Create a message
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setText(body);
        message.setSentDate(new Date());
        
        // Send the message
        Transport.send(message);
        
        System.out.println("Email sent successfully!");
    }
    
    /**
     * Sends an email with attachments.
     *
     * @param host SMTP server host
     * @param port SMTP server port
     * @param username Email account username
     * @param password Email account password
     * @param from Sender's email address
     * @param to Recipient's email address
     * @param subject Email subject
     * @param body Email body content
     * @param attachmentPaths Array of file paths to attach
     * @throws MessagingException If there's an error sending the email
     */
    public static void sendEmailWithAttachments(String host, String port,
                                               String username, String password,
                                               String from, String to,
                                               String subject, String body,
                                               String[] attachmentPaths) throws MessagingException {
        
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
        
        // Create a message
        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        message.setSentDate(new Date());
        
        // Create the message body part
        BodyPart messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText(body);
        
        // Create a multipart message
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);
        
        // Add attachments
        if (attachmentPaths != null && attachmentPaths.length > 0) {
            for (String filePath : attachmentPaths) {
                addAttachment(multipart, filePath);
            }
        }
        
        // Set the complete message parts
        message.setContent(multipart);
        
        // Send the message
        Transport.send(message);
        
        System.out.println("Email with attachments sent successfully!");
    }
    
    /**
     * Adds an attachment to the email.
     *
     * @param multipart The multipart to add the attachment to
     * @param filePath The path to the file to attach
     * @throws MessagingException If there's an error adding the attachment
     */
    private static void addAttachment(Multipart multipart, String filePath) throws MessagingException {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                System.out.println("File not found: " + filePath);
                return;
            }
            
            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.attachFile(file);
            multipart.addBodyPart(attachmentPart);
            
        } catch (Exception e) {
            throw new MessagingException("Error adding attachment: " + e.getMessage());
        }
    }
    
    /**
     * Example usage of the EmailSender class.
     */
    public static void main(String[] args) {
        // Example SMTP server details
        String host = "smtp.gmail.com";
        String port = "587";
        String username = "quangbinh1001@gmail.com";
        String password = "<APP_PWD>"; // Use app password for Gmail
        
        // Email details
        String from = "quangbinh1001@gmail.com";
        String to = "binh.le@trustifytechnology.com";
        String subject = "Test Email from Jakarta Mail";
        String body = "This is a test email sent using Jakarta Mail.";
        
        try {
            // Send a simple email
            sendSimpleEmail(host, port, username, password, from, to, subject, body);
            
            // Send an email with attachments
//            String[] attachments = {"path/to/file1.pdf", "path/to/file2.jpg"};
//            sendEmailWithAttachments(host, port, username, password, from, to,
//                                    "Test Email with Attachments", body, attachments);
            
        } catch (MessagingException e) {
            System.out.println("Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}