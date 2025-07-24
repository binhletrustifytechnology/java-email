package org.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class ExtractNormalEmailContentWithOpenAI {

    private static final Logger log = LoggerFactory.getLogger(ExtractNormalEmailContentWithOpenAI.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private String openaiModel = "gpt-3.5-turbo";
    private String openaiApiKey = "KEY";

    /**
     * Constructor with default OpenAI model (gpt-3.5-turbo)
     * 
     * @param apiKey OpenAI API key
     */
    public ExtractNormalEmailContentWithOpenAI(String apiKey) {
        this.openaiApiKey = apiKey;
    }

    /**
     * Constructor with custom OpenAI model
     * 
     * @param apiKey OpenAI API key
     * @param model OpenAI model to use
     */
    public ExtractNormalEmailContentWithOpenAI(String apiKey, String model) {
        this.openaiApiKey = apiKey;
        this.openaiModel = model;
    }

    /**
     * Extracts key information from an email using OpenAI's API
     * 
     * @return JSON string containing extracted information
     */
    public String extractContentWithOpenAI(String content) {
        String prompt = String.format(
                """
                You are an email parser designed to extract only the most recent reply from a email message with html format.

                Instructions:
                    - Return only the content written in the latest reply.
                    - Exclude any quoted text from previous messages (usually indicated by lines like "On [date], [name] wrote:" or "From:", "Sent:", "Subject:").
                    - Remove email headers, disclaimers, signatures, and repetitive greetings.
                    - If the content is in HTML, convert it to clean plain text, preserving line breaks and paragraph structure.
                    - If there is no recent reply content found in the provided email message, the content primarily consists of images only without any textual reply or message. Please consider content is empty string

                Content: \n\n
                %s
                """
                , content
        );

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", openaiModel);

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);

        requestBody.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(openaiApiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.openai.com/v1/chat/completions",
                    entity,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseBody.get("choices");

                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> choice = choices.get(0);
                    Map<String, String> messageResponse = (Map<String, String>) choice.get("message");
                    return messageResponse.get("content");
                }
            }
        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage(), e);
            System.out.println("Error calling OpenAI API: " + e.getMessage());
        }

        // Fallback to simple JSON if OpenAI fails
        return String.format(
                "{\"content\":\"%s\"}",
                content.replaceAll("\"", "\\\\\"").replaceAll("\n", "\\\\n")
        );
    }

    /**
     * Loads environment variables from a .env file.
     *
     * @return Properties object containing the environment variables
     * @throws IOException if the .env file cannot be read
     */
    public static Properties loadEnvFile() throws IOException {
        Properties properties = new Properties();

        // Check if .env file exists
        if (Files.exists(Paths.get(".env"))) {
            try (InputStream input = new FileInputStream(".env")) {
                // Load properties from the .env file
                properties.load(input);
            }
        } else {
            System.out.println("Warning: .env file not found. Using default values.");
        }

        return properties;
    }

    /**
     * Example usage of the ExtractEmailContentWithOpenAI class.
     */
    public static void main(String[] args) {
        try {
            // Load environment variables from .env file
            Properties envProperties = loadEnvFile();

            // Get OpenAI API key from .env file
            String apiKey = "your-api-key";
            if (envProperties.containsKey("OPENAI_KEY")) {
                apiKey = envProperties.getProperty("OPENAI_KEY");
                if (apiKey == null || apiKey.trim().isEmpty()) {
                    System.out.println("OPENAI_KEY is empty in .env file");
                    System.out.println("Please set your actual OpenAI API key in the .env file");
                    return;
                }
                System.out.println("Using OpenAI API key from .env file");
            } else {
                System.out.println("OPENAI_KEY not found in .env file, using default value");
                System.out.println("Please set your actual OpenAI API key in the .env file");
                return;
            }

            // Create an instance of ExtractEmailContentWithOpenAI
            ExtractNormalEmailContentWithOpenAI extractor = new ExtractNormalEmailContentWithOpenAI(apiKey);

            // Example email data
            String subject = "Meeting Confirmation: Project Review";
            String content1 = """
                <html>
                        <head>
                        <meta http-equiv="Content-Type" content="text/html; charset=Windows-1252">
                        <style type="text/css" style="display:none;"> P {margin-top:0;margin-bottom:0;} </style>
                        </head>
                        <body dir="ltr">
                        <div class="elementToProof" style="text-align: left; text-indent: 0px; line-height: 28px; background-color: rgb(255, 255, 255); margin: 13px 0px 3px; max-width: 590px; font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, Calibri, Helvetica, sans-serif; font-size: 12pt; color: black;">
                        <span style="font-weight: 600;">Travel Booking Platform – Features Overview</span></div>
                        <ol start="1" style="text-align: left; margin: 4px 0px 0px; background-color: rgb(255, 255, 255);">
                        <li style="font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, Calibri, Helvetica, sans-serif; font-size: 12pt; color: black;">
                        <div role="presentation" style="margin: 0px 0px 8px; display: block;"><span style="font-weight: 600;">Flight Booking</span></div>
                        </li><ul style="padding-bottom: 8px; padding-left: 0px; list-style-position: initial; list-style-type: disc;">
                        <li style="font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, Calibri, Helvetica, sans-serif; font-size: 12pt; color: black;">
                        <div role="presentation" style="margin: 0px;">Domestic Flights</div>
                        </li></ul>
                        <ol start="1" style="padding-bottom: 8px; padding-left: 0px; list-style-position: initial; list-style-type: disc;">
                        <li style="font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, Calibri, Helvetica, sans-serif; font-size: 12pt; color: black; display: block;">
                        <div role="presentation" style="margin: 0px;">&nbsp; &nbsp; &nbsp; + Test 1</div>
                        </li></ol>
                        <li style="font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, Calibri, Helvetica, sans-serif; font-size: 12pt; color: black; display: block;">
                        <div role="presentation" style="margin: 0px;">&nbsp; &nbsp; &nbsp; + Test 2&nbsp;</div>
                        </li></ol>
                        <div class="elementToProof" style="text-align: left; text-indent: 0px; background-color: rgb(255, 255, 255); margin: 0px; font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, Calibri, Helvetica, sans-serif; font-size: 12pt; color: black;">
                        <br>
                        </div>
                        <ol start="2" style="text-align: left; margin: 4px 0px 0px; background-color: rgb(255, 255, 255);">
                        <ul style="padding-bottom: 8px; padding-left: 0px; list-style-position: initial; list-style-type: disc;">
                        <ul style="margin: 0px; padding-left: 0px; list-style-position: initial; list-style-type: disc;">
                        <li style="font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, Calibri, Helvetica, sans-serif; font-size: 12pt; color: black;">
                        Budget Airlines</li></ul>
                        </ul>
                        <li style="font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, Calibri, Helvetica, sans-serif; font-size: 12pt; color: black;">
                        <div role="presentation" style="margin: 0px;">Full-Service Airlines</div>
                        </li><ul style="padding-bottom: 8px; padding-left: 0px; list-style-position: initial; list-style-type: disc;">
                        <li style="font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, Calibri, Helvetica, sans-serif; font-size: 12pt; color: black;">
                        International Flights</li><ul style="margin: 0px; padding-left: 0px; list-style-position: initial; list-style-type: disc;">
                        <li style="font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, Calibri, Helvetica, sans-serif; font-size: 12pt; color: black;">
                        Direct Flights</li><li style="font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, Calibri, Helvetica, sans-serif; font-size: 12pt; color: black;">
                        Connecting Flights</li></ul>
                        </ul>
                        </ol>
                        <div class="elementToProof" style="font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, Calibri, Helvetica, sans-serif; font-size: 12pt; color: rgb(0, 0, 0);">
                        <br>
                        </div>
                        <div class="elementToProof" style="font-family: Aptos, Aptos_EmbeddedFont, Aptos_MSFontService, Calibri, Helvetica, sans-serif; font-size: 12pt; color: rgb(0, 0, 0);">
                        <br>
                        </div>
                        </body>
                        </html>
            """;

            String content = """
            <html>
            <body>
            <div>
            testEmail mapping
            </div>
                        </body>
                        </html>
                        """;

            String sender = "john@example.com";
            Date receivedDate = new Date();

            // Extract content using OpenAI
            System.out.println("Extracting email content using OpenAI...");
            String extractedJson = extractor.extractContentWithOpenAI(content);

            // Display the extracted JSON
            System.out.println("\nExtracted JSON:\n");
            System.out.println(extractedJson);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
