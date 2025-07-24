package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

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

public class ExpediaEmailContentWithOpenAI {

    private static final Logger log = LoggerFactory.getLogger(ExpediaEmailContentWithOpenAI.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private String openaiModel = "gpt-3.5-turbo";
    private String openaiApiKey = "KEY";

    /**
     * Constructor with default OpenAI model (gpt-3.5-turbo)
     *
     * @param apiKey OpenAI API key
     */
    public ExpediaEmailContentWithOpenAI(String apiKey) {
        this.openaiApiKey = apiKey;
    }

    /**
     * Constructor with custom OpenAI model
     *
     * @param apiKey OpenAI API key
     * @param model OpenAI model to use
     */
    public ExpediaEmailContentWithOpenAI(String apiKey, String model) {
        this.openaiApiKey = apiKey;
        this.openaiModel = model;
    }

    /**
     * Extracts key information from an email using OpenAI's API
     * 
     * @param subject Email subject
     * @param content Email content
     * @param sender Email sender
     * @param receivedDate Email received date
     * @return JSON string containing extracted information
     */
    public String extractContentWithOpenAI(String subject, String content, String sender, Date receivedDate) {
        String prompt = String.format(
                "Extract key information from this email into JSON format with the following fields:\n" +
                        "- From\n" +
                        "- To\n" +
                        "- Subject\n" +
                        "- Date\n" +
                        "- PropertyID (if present)\n" +
                        "- Content (" +
                        "   Only include the user-written message body." +
                        "   Exclude any system notifications, UI elements, headers, replies, or boilerplate text." +
                        ")\n\n" +
                        "Email details:\n" +
                        "From: %s\n" +
                        "To: [Extract from email header]\n" +
                        "Subject: %s\n" +
                        "Date: %s\n" +
                        "Content: %s\n\n" +
                        "Return only a **valid JSON object** with relevant fields properly extracted."
//                """
//                    You are an advanced email parsing AI. Your task is to extract key information from an email and format it as a JSON object.
//
//                    **Instructions:**
//                    1.  **Extract all fields accurately.**
//                    2.  **For the 'Content' field, be extremely precise:**
//                        * **ONLY** include the core, user-written message body.
//                        * **EXCLUDE** any system notifications, UI elements, email headers, footers, previous replies (unless explicitly part of the current message), disclaimers, or boilerplate text (e.g., "Message from:", "Sent from my iPhone", "Confidentiality Notice").
//                        * If the email is a reply, focus only on the *new* message added by the sender, not the quoted previous email unless it's directly referenced and essential to the current message's context.
//
//                    **Desired JSON Schema:**
//                    ```json
//                    {
//                      "From": %s,
//                      "To": "[Extract from email header]",
//                      "Subject": %s,
//                      "Date": %s,
//                      "BookingID or Property ID": "string or null (if not present)",
//                      "Content": %s
//                    }
//                """
                , sender, subject, receivedDate, content
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
        return null;
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
            ExpediaEmailContentWithOpenAI extractor = new ExpediaEmailContentWithOpenAI(apiKey);

            // Example email data
            String subject = "Meeting Confirmation: Project Review";
            String content = """
                <html>
                        <head>
                        "	<meta http-equiv=""Content-Type"" content=""text/html; charset=UTF-8"">
                        "
                        	<title>
                        					Expedia guest message from Atichart Sirinan
                        	</title>
                        </head>
                        
                        "<body style=""font-family:-apple-system,BlinkMacSystemFont,'SF Pro Text','Segoe UI',Roboto,'Helvetica Neue',Arial,sans-serif; margin:0px; background-color: #ffffff;"">
                        "
                        
                        
                        "<table border=""0"" cellspacing=""0"" cellpadding=""0"" width=""600"" style=""margin: auto; width:600px; background-color: #eff1f3"">
                        "
                        	<!-- LOGO HEADER -->
                        	<tr>
                        "		<td bgcolor=""#000000"" style=""padding: 24px;"">
                        "
                        "			<div style=""display: inline-block;"">
                        "
                        "				<a href=""https://link.expediapartnercentral.com/c/7/eyJhaSI6NDc2ODk4ODcsImUiOiJ0aHJ1YmFja29mZmljZUBnbWFpbC5jb20iLCJycSI6IjAyLWIyNTE5OS0wODZhZGRiMTM0ZTE0YTdmYTk4YTA2ODQ2M2U2YWM2ZSIsIm0iOmZhbHNlLCJ1aSI6IjAiLCJ1biI6IiIsInUiOiJodHRwczovL2pvaW4uZXhwZWRpYXBhcnRuZXJjZW50cmFsLmNvbSJ9/O2snfa5akjX3xgVopj8zzw"" target=""_blank"">
                        "
                        "					<img src=""https://c.travel-assets.com/epc/pc-conversations-static-assets/images/eg-partnercentral-logo.png"" width=200 style=""display: block;outline: none;border: none""/>
                        "
                        				</a>
                        			</div>
                        "			<div style=""color: #ffffff; text-align: right; display: inline-block; font-size: 14px; float: right;"">
                        "
                        				<div>โรงแรมครูว์</div>
                        				<div>Property ID: 34011606</div>
                        			</div>
                        		</td>
                        	</tr>
                        	<!-- BODY -->
                        	<tr>
                        "		<td style=""padding: 24px;"">
                        "
                        "			<div style=""background-color: #ffffff; text-align: center; padding: 24px 12px 36px;"">
                        "
                        "				<div style=""padding-bottom: 50px;"">
                        "
                        "					<p style=""font-size: 18px; line-height: 22px; font-weight: 700; color: #121617; margin: 0;"">
                        "
                        							Atichart Sirinan sent you a message
                        					</p>
                        "						<p style=""color: #5E6F73; font-size: 16px; line-height: 22px; margin: 0; padding-top: 8px;"">
                        "
                        "							""Chat testing
                        "
                        
                        "Dear [Guest’s Name],
                        "
                        Thank you for choosing to stay with us at [Hotel Name]. We’re pleased to confirm your booking as follows:
                        Booking Details
                        Guest Name: [Guest’s Full Name]
                        Check-in Date: [Date]
                        Check-out Date: [Date]
                        Number of Guests: [Number]
                        Room Type: [Room Type]
                        Reservation Number: [Reservation No.]
                        "If you have any special requests or need further assistance before your arrival, please don’t hesitate to reach out. We look forward to welcoming you soon!
                        "
                        "Best regards,
                        "
                        [Your Name]
                        [Your Position]
                        [Hotel Name]
                        "[Contact Information]""
                        "
                        						</p>
                        				</div>
                        "				<a href=""https://link.expediapartnercentral.com/c/7/eyJhaSI6NDc2ODk4ODcsImUiOiJ0aHJ1YmFja29mZmljZUBnbWFpbC5jb20iLCJycSI6IjAyLWIyNTE5OS0wODZhZGRiMTM0ZTE0YTdmYTk4YTA2ODQ2M2U2YWM2ZSIsIm0iOmZhbHNlLCJ1aSI6IjEiLCJ1biI6IiIsInUiOiJodHRwczovL2FwcHMuZXhwZWRpYXBhcnRuZXJjZW50cmFsLmNvbS9sb2RnaW5nL2NvbnZlcnNhdGlvbnMvbWVzc2FnZUNlbnRlci5odG1sP2h0aWQ9MzQwMTE2MDYmY2lkPTNiYzcyYzQwLTgzMDItNGEwYy1hY2RmLTM3M2E0MTQ3MDZmNCZjcGNlUGFydG5lcklkPTgwZjc4ZmUwLWMzNjItNDkzZC05MmQ5LTg3N2EyOTIyZjJjZSJ9/xRffB7m0fB9v7a-ZI5zMfQ"" target=""_blank"" style=""background-color: #4C4CFF; padding: 15px 20px; text-decoration: none"">
                        "
                        "					<span style=""color:#ffffff; font-size: 14px; font-weight: 700;"">
                        "
                        							Reply
                        					</span>
                        				</a>
                        			</div>
                        		</td>
                        	</tr>
                        	<!-- PREVIOUS MESSAGES -->
                        	<!-- FOOTER -->
                        	<tr>
                        "		<td bgcolor=""#000000"">
                        "
                        "			<table cellspacing=""0"" cellpadding=""0"" border=""0"" align=""center"" width=""600"" stlye=""margin: auto; font-family: Arial,Helvetica,sans-serif; color: #ffffff; line-height:18px;"">
                        "
                        				<tbody>
                        					<tr>
                        "						<td bgcolor=""#000000"">
                        "
                        "							<table border=""0"" cellspacing=""0"" cellpadding=""0"" align=""center"" width=""100%"">
                        "
                        								<tbody>
                        									<tr>
                        										<td>
                        "											<table border=""0"" cellspacing=""0"" cellpadding=""0"" align=""center"" width=""100%"">
                        "
                        												<tbody>
                        													<tr>
                        "														<td width=""200"" style=""padding: 20px 20px 20px;"">
                        "
                        "															<img height=""30px"" src=""https://c.travel-assets.com/epc/pc-conversations-static-assets/images/eg-partnercentral-logo.png"">
                        "
                        														</td>
                        "														<td widht=""80"" align=""right"" style=""padding: 6px 20px 20px;"">
                        "
                        "															<table border=""0"" cellspacing=""0"" cellpadding=""0"" align=""right"">
                        "
                        																<tbody>
                        																	<tr>
                        "																		<td style=""padding-right: 30px;""><a href=""https://link.expediapartnercentral.com/c/7/eyJhaSI6NDc2ODk4ODcsImUiOiJ0aHJ1YmFja29mZmljZUBnbWFpbC5jb20iLCJycSI6IjAyLWIyNTE5OS0wODZhZGRiMTM0ZTE0YTdmYTk4YTA2ODQ2M2U2YWM2ZSIsIm0iOmZhbHNlLCJ1aSI6IjIiLCJ1biI6IiIsInUiOiJodHRwczovL3R3aXR0ZXIuY29tL0V4cGVkaWFMUFMifQ/pPSBFOmVmjQevupN67_Reg"" target=""_blank"" style=""outline:none;border:none;"">
                        "
                        "																			<img src=""http://mslps.expedia.com/images/dir/expedia-footer-twitter.png"" width=""20"" alt=""Expedia Twitter"" style=""display:block;"">
                        "
                        																		</a></td>
                        "																		<td style=""padding-right: 30px;""><a href=""https://link.expediapartnercentral.com/c/7/eyJhaSI6NDc2ODk4ODcsImUiOiJ0aHJ1YmFja29mZmljZUBnbWFpbC5jb20iLCJycSI6IjAyLWIyNTE5OS0wODZhZGRiMTM0ZTE0YTdmYTk4YTA2ODQ2M2U2YWM2ZSIsIm0iOmZhbHNlLCJ1aSI6IjMiLCJ1biI6IiIsInUiOiJodHRwczovL3d3dy5saW5rZWRpbi5jb20vY29tcGFueS9leHBlZGlhLWxvZGdpbmctcGFydG5lci1zZXJ2aWNlcyJ9/o6z6OC2Xl9M4nf9ssXHWoA"" target=""_blank"" style=""outline:none; border:none;"">
                        "
                        "																			<img src=""http://mslps.expedia.com/images/dir/expedia-footer-linkedin.png"" width=""20"" alt=""Expedia Linkedin"" style=""display:block;"">
                        "
                        																		</a></td>
                        "																		<td><a href=""https://link.expediapartnercentral.com/c/7/eyJhaSI6NDc2ODk4ODcsImUiOiJ0aHJ1YmFja29mZmljZUBnbWFpbC5jb20iLCJycSI6IjAyLWIyNTE5OS0wODZhZGRiMTM0ZTE0YTdmYTk4YTA2ODQ2M2U2YWM2ZSIsIm0iOmZhbHNlLCJ1aSI6IjQiLCJ1biI6IiIsInUiOiJodHRwczovL3d3dy5mYWNlYm9vay5jb20vZXhwZWRpYWZvcnByb3BlcnRpZXMifQ/0bE6vKgea3n2HFOV16OjMw"" target=""_blank"" style=""outline:none;border:none;"">
                        "
                        "																			<img src=""http://mslps.expedia.com/images/dir/expedia-footer-facebook.png"" width=""20"" alt=""Expedia Facebook"" style=""display:block;"">
                        "
                        																		</a></td>
                        																	</tr>
                        																</tbody>
                        															</table>
                        														</td>
                        													</tr>
                        													<tr>
                        "														<td style=""padding: 0px 20px 20px 20px; color: #ffffff;font-size:12px;"">
                        "
                        															<span>
                        "																© 2025 Expedia, Inc. All rights reserved. Confidential and proprietary.
                        "
                        															</span>
                        														</td>
                        													</tr>
                        													<tr>
                        "														<td style=""padding: 0px 20px 20px 20px; color: #ffffff;font-size:12px; width: 100%"">
                        "
                        "																I want to <a style=""color: #ffffff"" href=""https://link.expediapartnercentral.com/c/7/eyJhaSI6NDc2ODk4ODcsImUiOiJ0aHJ1YmFja29mZmljZUBnbWFpbC5jb20iLCJycSI6IjAyLWIyNTE5OS0wODZhZGRiMTM0ZTE0YTdmYTk4YTA2ODQ2M2U2YWM2ZSIsIm0iOmZhbHNlLCJ1aSI6IjUiLCJ1biI6IiIsInUiOiJodHRwczovL2FwcHMuZXhwZWRpYXBhcnRuZXJjZW50cmFsLmNvbS9sb2RnaW5nL25vdGlmaWNhdGlvbnMvdXNlci9wcm9wZXJ0eS9zZXR0aW5ncz9odGlkPTM0MDExNjA2In0/1oX6o5yxY-voqTlSuVyO-Q"">unsubscribe</a> from these emails
                        "
                        														</td>
                        													</tr>
                        												</tbody>
                        											</table>
                        										</td>
                        									</tr>
                        								</tbody>
                        							</table>
                        						</td>
                        					</tr>
                        				</tbody>
                        			</table>
                        		</td>
                        	</tr>
                        </table>
                        </div>
                        
                        "<table width=""1"" cellpadding=""0"" cellspacing=""0"" border=""0"" role=""presentation""><tr><td style=""font-size:0; line-height:0; mso-line-height-rule:exactly;""><img src=""https://link.expediapartnercentral.com/o/4/eyJhaSI6NDc2ODk4ODcsImUiOiJ0aHJ1YmFja29mZmljZUBnbWFpbC5jb20iLCJycSI6IjAyLWIyNTE5OS0wODZhZGRiMTM0ZTE0YTdmYTk4YTA2ODQ2M2U2YWM2ZSJ9/AXPMnnFOrx0IPQz6LWFeBg"" width=""1"" height=""1"" border=""0"" vspace=""0"" hspace=""0"" alt=""\"" /></td></tr></table></body>
                        "
                        </html>
            """;
            String sender = "6180354018-4FE2ABB1D79CODZ06VA3ADZ06.90FGCRE7B8T1145ZUYPVXET19@mchat.booking.com";
            Date receivedDate = new Date();

            // Extract content using OpenAI
            System.out.println("Extracting email content using OpenAI...");
            String extractedJson = extractor.extractContentWithOpenAI(subject, content, sender, receivedDate);

            // Display the extracted JSON
            System.out.println("\nExtracted JSON:");
            System.out.println(extractedJson);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
