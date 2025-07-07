# Jakarta Mail Email Sender

A simple Java application that demonstrates how to send emails using Jakarta Mail.

## Features

- Send simple text emails
- Send emails with attachments
- Support for SMTP authentication
- TLS encryption

## Dependencies

This project uses the following dependencies:

```xml
<dependency>
  <groupId>com.sun.mail</groupId>
  <artifactId>jakarta.mail</artifactId>
  <version>2.0.1</version>
</dependency>

<dependency>
  <groupId>com.sun.activation</groupId>
  <artifactId>jakarta.activation</artifactId>
  <version>2.0.1</version>
</dependency>
```

## How to Use

### Sending a Simple Email

```java
import org.example.EmailSender;
import jakarta.mail.MessagingException;

public class EmailExample {
    public static void main(String[] args) {
        // SMTP server details
        String host = "smtp.gmail.com";
        String port = "587";
        String username = "your.email@gmail.com";
        String password = "your-app-password"; // Use app password for Gmail
        
        // Email details
        String from = "your.email@gmail.com";
        String to = "recipient@example.com";
        String subject = "Test Email";
        String body = "This is a test email sent using Jakarta Mail.";
        
        try {
            EmailSender.sendSimpleEmail(host, port, username, password, from, to, subject, body);
            System.out.println("Email sent successfully!");
        } catch (MessagingException e) {
            System.out.println("Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

### Sending an Email with Attachments

```java
import org.example.EmailSender;
import jakarta.mail.MessagingException;

public class EmailWithAttachmentsExample {
    public static void main(String[] args) {
        // SMTP server details
        String host = "smtp.gmail.com";
        String port = "587";
        String username = "your.email@gmail.com";
        String password = "your-app-password"; // Use app password for Gmail
        
        // Email details
        String from = "your.email@gmail.com";
        String to = "recipient@example.com";
        String subject = "Test Email with Attachments";
        String body = "This is a test email with attachments sent using Jakarta Mail.";
        
        // Attachments
        String[] attachments = {
            "path/to/file1.pdf",
            "path/to/file2.jpg"
        };
        
        try {
            EmailSender.sendEmailWithAttachments(host, port, username, password, from, to, subject, body, attachments);
            System.out.println("Email with attachments sent successfully!");
        } catch (MessagingException e) {
            System.out.println("Failed to send email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

## Security Notes

### Gmail and App Passwords

If you're using Gmail as your SMTP server, you'll need to:

1. Enable 2-Step Verification for your Google account
2. Generate an App Password specifically for this application
3. Use that App Password instead of your regular Gmail password

Never hardcode passwords in your code. Consider using environment variables or a secure configuration file.

## Common SMTP Servers

- Gmail: smtp.gmail.com (Port 587 for TLS)
- Outlook/Hotmail: smtp.office365.com (Port 587 for TLS)
- Yahoo: smtp.mail.yahoo.com (Port 587 for TLS)

## Troubleshooting

- If you get authentication errors, check your username and password
- For Gmail, make sure you're using an App Password
- Some email providers may block emails sent from applications for security reasons
- Check your firewall settings if you're having connection issues