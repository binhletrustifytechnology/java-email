package org.example;

import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.FlagTerm;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Date;

/**
 * A utility class for reading emails from an IMAP server.
 */
public class EmailReader {

    /**
     * Reads the latest email from the specified IMAP folder.
     *
     * @param host IMAP server host (e.g., "imap.gmail.com")
     * @param port IMAP server port (e.g., "993" for SSL)
     * @param username Email account username
     * @param password Email account password
     * @param folderName Folder to read from (e.g., "INBOX")
     * @return The latest email message content
     * @throws MessagingException If there's an error reading the email
     */
    public static Message getLatestEmail(String host, String port, 
                                        String username, String password,
                                        String folderName) throws MessagingException {

        // Set mail server properties
        Properties properties = new Properties();
        properties.put("mail.imap.host", host);
        properties.put("mail.imap.port", port);
        properties.put("mail.imap.ssl.enable", "true");
        properties.put("mail.imap.auth", "true");

        // Create a mail session with authenticator
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        Store store = null;
        Folder folder = null;
        Message latestMessage = null;

        try {
            // Connect to the store
            store = session.getStore("imap");
            store.connect(host, username, password);

            // Open the folder
            folder = store.getFolder(folderName);
            folder.open(Folder.READ_ONLY);

            // Get the latest message
            Message[] messages = folder.getMessages();
            if (messages.length == 0) {
                System.out.println("No messages found in folder: " + folderName);
                return null;
            }

            // Get the latest message (last in the array)
            Message original = messages[messages.length - 1];

            // Create a copy of the message to return after closing the folder
            latestMessage = new MimeMessage((MimeMessage) original);

        } catch (MessagingException e) {
            System.out.println("Error reading emails: " + e.getMessage());
            throw e;
        } finally {
            // Close the folder and store
            try {
                if (folder != null && folder.isOpen()) {
                    folder.close(false);
                }
                if (store != null && store.isConnected()) {
                    store.close();
                }
            } catch (MessagingException e) {
                System.out.println("Error closing resources: " + e.getMessage());
            }
        }

        return latestMessage;
    }

    /**
     * Gets the content of a message as a string.
     *
     * @param message The email message
     * @return The content as a string
     * @throws MessagingException If there's an error processing the message
     * @throws IOException If there's an error reading the content
     */
    public static String getMessageContent(Message message) throws MessagingException, IOException {
        Object content = message.getContent();

        if (content instanceof String) {
            return (String) content;
        } else if (content instanceof MimeMultipart) {
            return getTextFromMimeMultipart((MimeMultipart) content);
        } else {
            return "Content type not supported: " + content.getClass().getName();
        }
    }

    /**
     * Extracts text content from a MimeMultipart object.
     *
     * @param mimeMultipart The MimeMultipart object
     * @return The extracted text
     * @throws MessagingException If there's an error processing the parts
     * @throws IOException If there's an error reading the content
     */
    private static String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
        StringBuilder result = new StringBuilder();
        int count = mimeMultipart.getCount();

        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result.append(bodyPart.getContent());
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                result.append(html);
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                result.append(getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent()));
            }
        }

        return result.toString();
    }

    /**
     * Displays information about an email message.
     *
     * @param message The email message
     * @throws MessagingException If there's an error processing the message
     * @throws IOException If there's an error reading the content
     */
    public static void displayEmailInfo(Message message) throws MessagingException, IOException {
        System.out.println("Subject: " + message.getSubject());
        System.out.println("From: " + message.getFrom()[0]);
        System.out.println("Date: " + message.getSentDate());
//        System.out.println("Content: " + getMessageContent(message));
    }

    /**
     * Gets unread messages from the specified folder.
     *
     * @param host IMAP server host
     * @param port IMAP server port
     * @param username Email account username
     * @param password Email account password
     * @param folderName Folder to read from
     * @return List of unread messages
     * @throws MessagingException If there's an error reading the emails
     */
    public static List<Message> getUnreadMessages(String host, String port,
                                                String username, String password,
                                                String folderName) throws MessagingException {
        // Set mail server properties
        Properties properties = new Properties();
        properties.put("mail.imap.host", host);
        properties.put("mail.imap.port", port);
        properties.put("mail.imap.ssl.enable", "true");
        properties.put("mail.imap.auth", "true");

        // Create a mail session with authenticator
        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        Store store = null;
        Folder folder = null;
        List<Message> unreadMessages = new ArrayList<>();

        try {
            // Connect to the store
            store = session.getStore("imap");
            store.connect(host, username, password);

            // Open the folder
            folder = store.getFolder(folderName);
            folder.open(Folder.READ_ONLY);

            // Search for unread messages
            FlagTerm flagTerm = new FlagTerm(new Flags(Flags.Flag.SEEN), false);
            Message[] messages = folder.search(flagTerm);

            // Create copies of the messages to return after closing the folder
            for (Message message : messages) {
                unreadMessages.add(new MimeMessage((MimeMessage) message));
            }

        } catch (MessagingException e) {
            System.out.println("Error reading unread emails: " + e.getMessage());
            throw e;
        } finally {
            // Close the folder and store
            try {
                if (folder != null && folder.isOpen()) {
                    folder.close(false);
                }
                if (store != null && store.isConnected()) {
                    store.close();
                }
            } catch (MessagingException e) {
                System.out.println("Error closing resources: " + e.getMessage());
            }
        }

        return unreadMessages;
    }

    /**
     * Closes the connection to the mail store.
     *
     * @param store The mail store to close
     */
    public static void closeConnection(Store store) {
        if (store != null && store.isConnected()) {
            try {
                store.close();
            } catch (MessagingException e) {
                System.out.println("Error closing store: " + e.getMessage());
            }
        }
    }

    /**
     * Example usage of the EmailReader class.
     */
    public static void main(String[] args) {
        // Example IMAP server details for Gmail
        String host = "imap.gmail.com";
        String port = "993"; // SSL port
        String username = "quangbinh1001@gmail.com";
        String password = "<APP_PWD>"; // Use app password for Gmail
        String folderName = "INBOX";

        try {
            // Get the latest email
            System.out.println("Fetching latest email...");
            Message latestEmail = getLatestEmail(host, port, username, password, folderName);

            if (latestEmail != null) {
                // Display email information
                System.out.println("\n--- Latest Email ---");
                displayEmailInfo(latestEmail);
            }



            // Get unread emails
//            System.out.println("\nFetching unread emails...");
//            List<Message> unreadMessages = getUnreadMessages(host, port, username, password, folderName);
//
//            if (unreadMessages.isEmpty()) {
//                System.out.println("No unread emails found.");
//            } else {
//                System.out.println("Found " + unreadMessages.size() + " unread emails:");
//
//                // Display the first 5 unread emails or fewer if there are less than 5
//                int displayCount = Math.min(unreadMessages.size(), 5);
//                for (int i = 0; i < displayCount; i++) {
//                    System.out.println("\n--- Unread Email " + (i + 1) + " ---");
//                    displayEmailInfo(unreadMessages.get(i));
//                }
//            }

        } catch (MessagingException | IOException e) {
            System.out.println("Failed to read email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
