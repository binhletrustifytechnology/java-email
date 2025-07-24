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

public class ExtractOTAEmailContentWithOpenAI {

    private static final Logger log = LoggerFactory.getLogger(ExtractOTAEmailContentWithOpenAI.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private String openaiModel = "gpt-3.5-turbo";
    private String openaiApiKey = "KEY";

    /**
     * Constructor with default OpenAI model (gpt-3.5-turbo)
     *
     * @param apiKey OpenAI API key
     */
    public ExtractOTAEmailContentWithOpenAI(String apiKey) {
        this.openaiApiKey = apiKey;
    }

    /**
     * Constructor with custom OpenAI model
     *
     * @param apiKey OpenAI API key
     * @param model OpenAI model to use
     */
    public ExtractOTAEmailContentWithOpenAI(String apiKey, String model) {
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
                        "- BookingID or Property ID (if present)\n" +
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
            ExtractOTAEmailContentWithOpenAI extractor = new ExtractOTAEmailContentWithOpenAI(apiKey);

            // Example email data
            String subject = "Meeting Confirmation: Project Review";
            String content = """
                "<!DOCTYPE html PUBLIC ""-//W3C//DTD XHTML 1.0 Transitional//EN"" ""http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd"">"
                               "<html xmlns=""http://www.w3.org/1999/xhtml"" xmlns:v=""urn:schemas-microsoft-com:vml"">"
                                <head>
                                 <title>เราได้รับข้อความจากคุณ Somruethai PA</title>
                               "  <style type=""text/css"">"
                               .ExternalClass {width:100%;}
                               ".ExternalClass, .ExternalClass p, .ExternalClass span, .ExternalClass font, .ExternalClass td, .ExternalClass div {"
                               line-height: 100% !important;
                               }
                               body {-webkit-text-size-adjust:none; -ms-text-size-adjust:none;}
                               body {margin:0; padding:0;}
                               table td {border-collapse:collapse;}
                               u + .body { min-width: 420px; margin:0; padding:0; }
                               p {margin:0; padding:0; margin-bottom:0;}
                               "h1, h2, h3, h4, h5, h6 {"
                               color: black;
                               line-height: 100%;
                               }
                               "a, a:link {"
                               color: #0071C2;
                               text-decoration: underline;
                               }
                               "body, table, td, a { -webkit-text-size-adjust: 100%; -ms-text-size-adjust: 100%; }"
                               "table, td { mso-table-lspace: 0pt; mso-table-rspace: 0pt; }"
                               img { -ms-interpolation-mode: bicubic; }
                               img { border: 0; outline: none; text-decoration: none; }
                               .hidden {
                               display: none !important;
                               }
                               "* [lang=""container-table""] {"
                               max-width: 420px;
                               }
                               '"@media only screen and (max-width: 500px ) {"
                               .mobile-grid {
                               margin: 0 auto !important;
                               width: 100% !important;
                               display: block !important;
                               }
                               .mobile-1-col {
                               width: 100% !important;
                               display: block !important;
                               padding-left: 20px !important;
                               padding-right: 20px !important;
                               min-width: 210px !important;
                               }
                               }
                               "/*body, table, td, a{-webkit-text-size-adjust: 100%; -ms-text-size-adjust: 100%; -webkit-font-smoothing: antialiased;}"
                               "table, td{mso-table-lspace: 0pt; mso-table-rspace: 0pt;}"
                               img{border: 0; height: auto; line-height: 100%; outline: none; text-decoration: none; -ms-interpolation-mode: bicubic;}
                               table{border-collapse: collapse !important;}
                               "body{font-family:BlinkMacSystemFont, -apple-system, Segoe UI, Roboto, Helvetica, Arial, sans-serif; height: 100% !important; margin: 0 !important; padding: 0 !important; width: 100% !important; font-size: 14px; line-height: 20px;}"
                               "div[style*=""margin: 16px 0;""] { margin:0 !important; }"
                               a {color: #0071C2;}
                               p {margin: 0;}
                               a[x-apple-data-detectors] {
                               color: inherit !important;
                               text-decoration: none !important;
                               font-size: inherit !important;
                               font-family: inherit !important;
                               font-weight: inherit !important;
                               line-height: inherit !important;
                               }
                               .font-sm {font-size: 12px; line-height: 18px;}
                               .pulsepromo {background-color: #F5F5F5;}
                               .pulsepromo__badge {width: 119px; height: 35px;}
                               '"@media only screen and (min-width: 480px ) {"
                               body[body] .mobilecontent{display: table-cell !important; max-height: none !important;}
                               .booking-details {text-align: right;}
                               .message__legal {padding: 0 0 16px;}
                               }
                               '"@media only screen and (max-width: 479px ) {"
                               body[body] .m-hide{width:0;display:none!important;}
                               body[body] .m-show{display:block!important;}
                               body[body] .deviceWidth{width:95%!important;padding:0!important;}
                               body[body] .m-100p {width:100%!important;padding:0!important;}
                               body[body] .m-50p{width:49%!important;padding:0!important;}
                               .header td {display: block;}
                               .m-100p {width:90%!important;padding:0!important;}
                               .m-centre {float: none !important; text-align: center !important;}
                               .m-left {float: none !important; text-align: left !important; padding-top:20px !important;}
                               .m-right {float: none !important; text-align: right !important;}
                               .m-small {font-size: 16px !important; line-height: 20px !important;}
                               .m-pull-left {float:none !important; text-align: left !important;}
                               }*/
                               </style>
                                </head>
                               " <body bgcolor=""#FFFFFF"" class=""body"" leftmargin=""0"" marginheight=""0"" marginwidth=""0"" style=""font-family:BlinkMacSystemFont, -apple-system, Segoe UI, Roboto, Helvetica, Arial, sans-serif; "
                               font-size: 14px;
                               line-height: 20px;
                               font-weight: 400;
                               " background-color: #FFFFFF;width: 100%;"" topmargin=""0""><span style=""padding: 4px; "
                               font-size: 14px;
                               line-height: 20px;
                               font-weight: 400;
                               " color: #BDBDBD;"">"
                               ##- โปรดพิมพ์คำตอบกลับของท่านเหนือข้อความนี้ -##
                               "</span><table align=""center"" border=""0"" cellpadding=""0"" cellspacing=""0"" width=""100%"">"
                                  <tr>
                                   <td>
                               "     <div class=""Gmail"" style=""height: 1px !important; margin-top: -1px !important; max-width: 420 !important; min-width: 420 !important; width: 420 !important;"">"
                                    </div>
                                   </td>
                                  </tr>
                                  <tr>
                                   <td>
                               "     <table align=""center"" border=""0"" cellpadding=""0"" cellspacing=""0"" class=""container-table"" lang=""container-table"" style=""margin-left: auto; margin-right: auto; max-width: 420px; width: 420px; -webkit-font-smoothing: antialiased; -moz-font-smoothing: grayscale;"" width=""420"">"
                                     <tr>
                                      <td>
                               "        <table bgcolor=""#FFFFFF"" border=""0"" cellpadding=""0"" cellspacing=""0"" style=""width: 420px; border-bottom: 1px solid #E6E6E6; color: #333333;"" width=""420"">"
                                        <tr>
                               "          <td bgcolor=""#FFFFFF"" style=""padding-left: 20px; padding-top: 10px; padding-bottom: 10px; background-color: #FFFFFF;""><img alt=""\"" border=""0"" src=""cid:27329730581bcdd62170033ed9a4bc63fed7e4baf114bfe3b0c2b757d56084bf@MIME-Lite-HTML-1.25"" style=""display: block; margin: 0; padding: 0; width: 130px;"" width=""130"" /></td>"
                               "          <td style=""padding-right: 20px; padding-top: 10px; padding-bottom: 10px; background-color: #FFFFFF;"">"
                               "           <p style=""\"
                               font-size: 14px;
                               line-height: 20px;
                               font-weight: 400;
                               " margin: 0; padding: 0; text-align: right; color: #333333;"">"
                               หมายเลขยืนยันการจอง:
                               "<a href=""https://admin.booking.com/hotel/hoteladmin/extranet_ng/manage/booking.html?hotel_id=4571043&amp;res_id=6180354018&amp;email=1&amp;lang=th&amp;utm_campaign=pf_guest_request&amp;utm_term=free_text&amp;utm_medium=email&amp;utm_source=messaging&amp;utm_content=booknr&amp;_e=1752725690&amp;_s=0vKcIdKeMLYMCvX+u36XOK2CP4S6YJVDbLRzZQ/j/2M"" style="" display: inline-block;"">"
                               6180354018
                               </a></td>
                                        </tr>
                                       </table>
                               "        <table align=""center"" border=""0"" cellpadding=""0"" cellspacing=""0"" style=""width: 420px;"" width=""420"">"
                                        <tr>
                               "          <td style=""\"
                               font-size: 24px;
                               line-height: 32px;
                               font-weight: 600;
                               " font-weight: 700; text-align: center; color: #333333; padding-top: 50px; padding-left: 80px; padding-right: 80px; padding-bottom: 50px;"">"
                               ท่านได้รับข้อความใหม่จากลูกค้า!
                               </td>
                                        </tr>
                                       </table>
                               "        <table border=""0"" cellpadding=""0"" cellspacing=""0"" width=""100%"">"
                                        <tr>
                               "          <td style=""padding-left: 20px; padding-bottom: 10px; padding-right: 20px; text-align: left;"">"
                               "           <p style=""\"
                               font-size: 16px;
                               line-height: 24px;
                               font-weight: 500;
                               \s
                               display: block;
                               margin: 0;
                               padding: 0;
                               " font-weight: 600; color: #333333;"">"
                               ข้อความจากคุณ Somruethai PA:
                               </td>
                                        </tr>
                                       </table>
                               "        <table align=""center"" border=""0"" cellpadding=""0"" cellspacing=""0"" style=""width: 420px;"" width=""420"">"
                                        <tr>
                               "          <td style=""padding-left: 10px;""><img alt=""\"" src=""cid:7e095f3873ccf55aeb4458950669711c2ded7b2644dd1620d53666f6eff02f90@MIME-Lite-HTML-1.25"" style=""display: block; margin: 0; padding: 0; width: 10px;"" width=""10"" /></td>"
                               "          <td style=""padding-right: 20px; padding-bottom: 20px;"" width=""100%"">"
                               "           <table align=""center"" border=""0"" cellpadding=""0"" cellspacing=""0"" width=""100%"">"
                                           <tr>
                               "             <td bgcolor=""#E6E6E6"" style=""\"
                               padding: 16px; margin: 0; background-color: #E6E6E6;\s
                               font-size: 16px;
                               line-height: 24px;
                               font-weight: 400;
                                color: #000000;
                               border-top-left-radius: 8px; border-top-right-radius: 8px;
                               border-bottom-left-radius: 8px; border-bottom-right-radius: 8px;
                               ""\"><span>Chat test from BDC5"
                               </span></td>
                                           </tr>
                                          </table>
                                         </td>
                                        </tr>
                                       </table>
                               "        <table align=""center"" border=""0"" cellpadding=""0"" cellspacing=""0"" width=""100%"">"
                                        <tr>
                               "          <td style=""padding: 20px;"">"
                               "           <table align=""left"" border=""0"" cellpadding=""0"" cellspacing=""0"" style=""\"
                               background-color: #0071C2;
                               border-radius:4px;
                               '"-webkit-text-size-adjust:none;"
                               color:#FFFFFF;
                               border: 2px solid #0071C2;
                               margin-right: 5px;
                               ""\">"
                                           <tr>
                               "             <td style=""\"
                               color: #FFFFFF;
                               font-size: 14px;
                               font-weight: 700;
                               text-align: center;
                               text-decoration:none;
                               padding: 10px;
                               ""\"><a alt=""https://admin.booking.com/hotel/hoteladmin/extranet_ng/manage/messaging/inbox.html?message_id=4aa7ac20-62c4-11f0-8352-d3e6330e1274&amp;amp;lang=th&amp;amp;from_instant_email=1&amp;amp;res_id=6180354018&amp;amp;hotel_id=4571043&amp;amp;product_id=6180354018&amp;amp;product_type=POST_BOOKING&amp;amp;utm_campaign=pf_guest_request&amp;amp;utm_term=free_text&amp;amp;utm_medium=email&amp;amp;utm_source=messaging&amp;amp;utm_content=reply&amp;amp;_e=1752725690&amp;amp;_s=X93q7YyUpJ7VGwnDwxuVzTm7jexI8BVQRUqtKKmZuhE"" href=""https://admin.booking.com/hotel/hoteladmin/extranet_ng/manage/messaging/inbox.html?message_id=4aa7ac20-62c4-11f0-8352-d3e6330e1274&amp;amp;lang=th&amp;amp;from_instant_email=1&amp;amp;res_id=6180354018&amp;amp;hotel_id=4571043&amp;amp;product_id=6180354018&amp;amp;product_type=POST_BOOKING&amp;amp;utm_campaign=pf_guest_request&amp;amp;utm_term=free_text&amp;amp;utm_medium=email&amp;amp;utm_source=messaging&amp;amp;utm_content=reply&amp;amp;_e=1752725690&amp;amp;_s=X93q7YyUpJ7VGwnDwxuVzTm7jexI8BVQRUqtKKmZuhE"" style=""\"
                               color: #FFFFFF;
                               font-size: 14px;
                               font-weight: 700;
                               text-align: center;
                               padding: 10px;
                               text-decoration:none;
                               cursor: pointer;
                               ""\" target=""_blank"">"
                               ตอบกลับ
                               </a></td>
                                           </tr>
                                          </table>
                               "           <p class=""hidden"" style=""\"
                               display: block;
                               margin: 0;
                               padding: 0;
                               " display: none; max-height: 0px; font-size: 0px; overflow: hidden; mso-hide: all; line-height:0px; max-width: 420;""> --&gt; https://admin.booking.com/hotel/hoteladmin/extranet_ng/manage/messaging/inbox.html?message_id=4aa7ac20-62c4-11f0-8352-d3e6330e1274&amp;lang=th&amp;from_instant_email=1&amp;res_id=6180354018&amp;hotel_id=4571043&amp;product_id=6180354018&amp;product_type=POST_BOOKING&amp;utm_campaign=pf_guest_request&amp;utm_term=free_text&amp;utm_medium=email&amp;utm_source=messaging&amp;utm_content=reply&amp;_e=1752725690&amp;_s=X93q7YyUpJ7VGwnDwxuVzTm7jexI8BVQRUqtKKmZuhE</td>"
                                        </tr>
                                       </table>
                               "        <table border=""0"" cellpadding=""0"" cellspacing=""0"" style=""width: 420px; border-top: 1px solid #E6E6E6;"" width=""420"">"
                                        <tr>
                                         <td>
                               "           <table border=""0"" cellpadding=""0"" cellspacing=""0"" width=""100%"">"
                                           <tr>
                               "             <td style=""\"
                               font-size: 14px;
                               line-height: 20px;
                               font-weight: 400;
                               " color: #6B6B6B; padding: 20px;"">"
                               "              <p style=""\"
                               font-size: 16px;
                               line-height: 24px;
                               font-weight: 500;
                               " margin: 0; color: #6B6B6B; font-weight: bold;"">"
                               ข้อมูลการจอง
                               </td>
                                           </tr>
                                          </table>
                               "           <table border=""0"" cellpadding=""0"" cellspacing=""0"" width=""100%"">"
                                           <tr>
                               "             <td style=""\"
                               font-size: 14px;
                               line-height: 20px;
                               font-weight: 400;
                               " color: #6B6B6B; padding: 0 20px 20px;"">"
                               "              <p style=""\"
                               display: block;
                               margin: 0;
                               padding: 0;
                               ""\">"
                               ชื่อผู้เข้าพัก:
                               "<br /><span style=""\"
                               display: block;
                               margin: 0;
                               padding: 0;
                               \s
                               font-size: 16px;
                               line-height: 24px;
                               font-weight: 500;
                               ""\">"
                               Somruethai PA
                               </span></td>
                                           </tr>
                                          </table>
                               "           <table border=""0"" cellpadding=""0"" cellspacing=""0"" width=""100%"">"
                                           <tr>
                               "             <td style=""padding-left: 20px; padding-bottom: 20px; padding-right: 20px;"">"
                               "              <table border=""0"" cellpadding=""0"" cellspacing=""0"" width=""100%"">"
                                              <tr>
                               "                <td style=""\"
                               font-size: 14px;
                               line-height: 20px;
                               font-weight: 400;
                               " color: #6B6B6B;"" width=""50%"">"
                               "                 <p style=""\"
                               display: block;
                               margin: 0;
                               padding: 0;
                               ""\">"
                               เช็คอิน:
                               "<br /><span style=""\"
                               display: block;
                               margin: 0;
                               padding: 0;
                               \s
                               font-size: 16px;
                               line-height: 24px;
                               font-weight: 500;
                               ""\">"
                               อังคาร 9 ก.ย. 2025
                               </span></td>
                               "                <td style=""\"
                               font-size: 14px;
                               line-height: 20px;
                               font-weight: 400;
                               " color: #6B6B6B;"" width=""50%"">"
                               "                 <p style=""\"
                               display: block;
                               margin: 0;
                               padding: 0;
                               ""\">"
                               เช็คเอาท์:
                               "<br /><span style=""\"
                               display: block;
                               margin: 0;
                               padding: 0;
                               \s
                               font-size: 16px;
                               line-height: 24px;
                               font-weight: 500;
                               ""\">"
                               พุธ 10 ก.ย. 2025
                               </span></td>
                                              </tr>
                                             </table>
                                            </td>
                                           </tr>
                                          </table>
                               "           <table border=""0"" cellpadding=""0"" cellspacing=""0"" width=""100%"">"
                                           <tr>
                               "             <td style=""\"
                               font-size: 14px;
                               line-height: 20px;
                               font-weight: 400;
                               " color: #6B6B6B; padding: 0 20px 20px;"">"
                               "              <p style=""\"
                               display: block;
                               margin: 0;
                               padding: 0;
                               ""\">"
                               ชื่อที่พัก:
                               "<br /><span style=""\"
                               display: block;
                               margin: 0;
                               padding: 0;
                               \s
                               font-size: 16px;
                               line-height: 24px;
                               font-weight: 500;
                               ""\">"
                               "<a href=""https://admin.booking.com/hotel/hoteladmin/extranet_ng/manage/home.html?lang=th&amp;hotel_id=4571043&amp;utm_campaign=pf_guest_request&amp;utm_term=free_text&amp;utm_medium=email&amp;utm_source=messaging&amp;utm_content=property_name&amp;_e=1752725690&amp;_s=fOZsYAxkjqct4qKcRfneJwE+0uV0Onf+LZ0jMYzPf2Y"" style=""\"
                               font-size: 16px;
                               line-height: 24px;
                               font-weight: 500;
                               \s
                               cursor: pointer;
                               color: #0071C2;
                               " text-decoration: none;"">"
                               CREW Hotel
                               </a>
                               </span></td>
                                           </tr>
                                          </table>
                               "           <table border=""0"" cellpadding=""0"" cellspacing=""0"" width=""100%"">"
                                           <tr>
                               "             <td style=""\"
                               font-size: 14px;
                               line-height: 20px;
                               font-weight: 400;
                               " color: #6B6B6B; padding: 0 20px 20px;"">"
                               "              <p style=""\"
                               display: block;
                               margin: 0;
                               padding: 0;
                               ""\">"
                               หมายเลขการจอง:
                               "<br /><span style=""\"
                               display: block;
                               margin: 0;
                               padding: 0;
                               \s
                               font-size: 16px;
                               line-height: 24px;
                               font-weight: 500;
                               ""\">"
                               "<a href=""https://admin.booking.com/hotel/hoteladmin/extranet_ng/manage/booking.html?hotel_id=4571043&amp;res_id=6180354018&amp;email=1&amp;lang=th&amp;utm_campaign=pf_guest_request&amp;utm_term=free_text&amp;utm_medium=email&amp;utm_source=messaging&amp;utm_content=booknr&amp;_e=1752725690&amp;_s=0vKcIdKeMLYMCvX+u36XOK2CP4S6YJVDbLRzZQ/j/2M"" style=""\"
                               font-size: 16px;
                               line-height: 24px;
                               font-weight: 500;
                               \s
                               cursor: pointer;
                               color: #0071C2;
                               " text-decoration: none;"">"
                               6180354018
                               </a>
                               </span></td>
                                           </tr>
                                          </table>
                               "           <table border=""0"" cellpadding=""0"" cellspacing=""0"" width=""100%"">"
                                           <tr>
                               "             <td style=""padding-left: 20px; padding-bottom: 20px; padding-right: 20px;"">"
                               "              <table border=""0"" cellpadding=""0"" cellspacing=""0"" width=""100%"">"
                                              <tr>
                               "                <td style=""\"
                               font-size: 14px;
                               line-height: 20px;
                               font-weight: 400;
                               " color: #6B6B6B;"" width=""50%"">"
                               "                 <p style=""\"
                               display: block;
                               margin: 0;
                               padding: 0;
                               ""\">"
                               จำนวนผู้เข้าพักทั้งหมด:
                               "<br /><span style=""\"
                               display: block;
                               margin: 0;
                               padding: 0;
                               \s
                               font-size: 16px;
                               line-height: 24px;
                               font-weight: 500;
                               ""\">"
                               2
                               </span></td>
                               "                <td style=""\"
                               font-size: 14px;
                               line-height: 20px;
                               font-weight: 400;
                               " color: #6B6B6B;"" width=""50%"">"
                               "                 <p style=""\"
                               display: block;
                               margin: 0;
                               padding: 0;
                               ""\">"
                               จำนวนห้องทั้งหมดที่จอง:
                               "<br /><span style=""\"
                               display: block;
                               margin: 0;
                               padding: 0;
                               \s
                               font-size: 16px;
                               line-height: 24px;
                               font-weight: 500;
                               ""\">"
                               1
                               </span></td>
                                              </tr>
                                             </table>
                                            </td>
                                           </tr>
                                          </table>
                                         </td>
                                        </tr>
                                       </table>
                               "        <table border=""0"" cellpadding=""0"" cellspacing=""0"" style=""width: 420px; color: #000000; border-top: 1px solid #E6E6E6;"" width=""100%"">"
                                        <tr>
                               "          <td align=""center"" style=""padding: 20px;"">"
                               "           <p style=""\"
                               display: block;
                               margin: 0;
                               padding: 0;
                               " font-size: 12px;"">"
                               "© ลิขสิทธิ์ <a href=""https://www.booking.com/?source=p2g_email"" style=""color:#0071C2;"">Booking.com</a> 2025<br />"
                               "อีเมลนี้มาจาก <a href=""https://www.booking.com/?source=p2g_email"" style=""color:#0071C2;"">Booking.com</a></td>"
                                        </tr>
                                        <tr>
                               "          <td align=""center"" style=""padding: 30px; padding-bottom: 20px;"">"
                               "           <p style=""\"
                               display: block;
                               margin: 0;
                               padding: 0;
                               \s
                               font-size: 12px;
                               line-height: 18px;
                               font-weight: 400;
                               " "">"
                               ขณะนี้ท่านได้สมัครรับอีเมลแจ้งเตือนจาก Booking.com ทราบหรือไม่ว่าท่านสามารถแก้ไขการตั้งค่าอีเมลและตั้งค่าการตอบกลับข้อความลูกค้าบางท่านโดยอัตโนมัติได้?
                               </td>
                                        </tr>
                                        <tr>
                               "          <td align=""center"" style=""padding: 10px;"">"
                               "           <p style=""\"
                               display: block;
                               margin: 0;
                               padding: 0;
                               \s
                               font-size: 12px;
                               line-height: 18px;
                               font-weight: 400;
                               " "">"
                               อีเมลฉบับนี้ส่งไปยัง: thrubackoffice@gmail.com
                               </td>
                                        </tr>
                                        <tr>
                               "          <td align=""center"" style=""padding: 10px;""><a href=""https://admin.booking.com/hotel/hoteladmin/extranet_ng/manage/messaging/settings.html?source=notifications&amp;lang=th&amp;hotel_id=4571043&amp;pm_view_route=settings%2Fnotifications&amp;utm_campaign=pf_guest_request&amp;utm_term=free_text&amp;utm_medium=email&amp;utm_source=messaging&amp;utm_content=edit_preferences&amp;_e=1752725690&amp;_s=e0UQCQdoBHp7lNBHpu7a3dRn7WZkI+wAcLyD9DxSgVM"" style=""\"
                               cursor: pointer;
                               color: #0071C2;
                               " font-size: 12px;"">"
                               แก้ไขการตั้งค่า
                               </a></td>
                                        </tr>
                                        <tr>
                               "          <td align=""center"" style=""padding: 20px;"">"
                               "           <p style=""\"
                               display: block;
                               margin: 0;
                               padding: 0;
                               \s
                               font-size: 12px;
                               line-height: 18px;
                               font-weight: 400;
                               ""\">"
                               "*Booking.com จะได้รับคำตอบกลับอีเมลฉบับนี้และจะดำเนินการตามที่ได้กำหนดไว้ใน<a href=""https://admin.booking.com/hotel/hoteladmin/privacy.html?lang=th&amp;utm_campaign=pf_guest_request&amp;utm_term=free_text&amp;utm_medium=email&amp;utm_source=messaging&amp;utm_content=privacy&amp;_e=1752725690&amp;_s=iXasYQ3phFeNwmdkspbHSRAFiHSN0dkc1P1u6+/DgBw"" style=""\"
                               cursor: pointer;
                               color: #0071C2;
                               ""\">แถลงการณ์ความเป็นส่วนตัวและการใช้คุกกี้</a>ของ Booking.com Booking.com ไม่ได้เป็นผู้เขียนเนื้อหาในข้อความนี้ จึงจะไม่รับผิดชอบหรือมีส่วนต้องรับผิดชอบต่อข้อความดังกล่าว"
                               </td>
                                        </tr>
                                       </table>
                               "        <style data-ignore-inlining=""data-ignore-inlining"">@media print{ #_t { background-image: url('https://fibjvw3g.emltrk.com/fibjvw3g?p&d=ba07b78acc89bb81f28faac11415b544');}} div.OutlookMessageHeader {background-image:url('https://fibjvw3g.emltrk.com/fibjvw3g?f&d=ba07b78acc89bb81f28faac11415b544')} table.moz-email-headers-table {background-image:url('https://fibjvw3g.emltrk.com/fibjvw3g?f&d=ba07b78acc89bb81f28faac11415b544')} blockquote #_t {background-image:url('https://fibjvw3g.emltrk.com/fibjvw3g?f&d=ba07b78acc89bb81f28faac11415b544')} #MailContainerBody #_t {background-image:url('https://fibjvw3g.emltrk.com/fibjvw3g?f&d=ba07b78acc89bb81f28faac11415b544')}</style>"
                               "        <div id=""_t"">"
                               "        </div><img alt=""\"" border=""0"" height=""1"" src=""https://fibjvw3g.emltrk.com/fibjvw3g?d=ba07b78acc89bb81f28faac11415b544"" width=""1"" /></td>"
                                     </tr>
                                    </table>
                                   </td>
                                  </tr>
                                 </table>
                               "  <div style=""display:none; white-space:nowrap; font:15px courier; line-height:0;"">                                                             </div><img height=""1"" src=""https://secure.booking.com/email_opened_tracking_pixel?lang=th&aid=304142&token=52616e646f6d49562473646523287d61fd4ea1866396e343913ea53e2a13d4f02fdf15d3dcd5d01c4cee0c656c335a8fd2c70da096e295399d441294c1b59684801993d758817aab70829ac56adac558396c78009b9213adc3af8a7f9320211828efc54a5dfba1a08eca4ad3f88a059cf368c9723808e7a1ddbf2ca0ba06d74e64d7294f55c74069&type=to_hotel_free_text"" width=""1"" /></body>"
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
