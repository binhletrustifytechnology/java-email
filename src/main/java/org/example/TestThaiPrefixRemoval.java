package org.example;

import java.io.IOException;
import java.util.Properties;

public class TestThaiPrefixRemoval {
    public static void main(String[] args) {
        try {
            // Load environment variables from .env file
            Properties envProperties = ExtractNormalEmailContentWithOpenAI.loadEnvFile();

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

            // Test case with Thai prefix
            String contentWithThaiPrefix = "ข้อความจากคุณ Somruethai PA: Chat test from BDC5";
            
            // Extract content using OpenAI
            System.out.println("Extracting email content using OpenAI...");
            String extractedJson = extractor.extractContentWithOpenAI(contentWithThaiPrefix);

            // Display the extracted JSON
            System.out.println("\nExtracted JSON:\n");
            System.out.println(extractedJson);

            // Test the fallback mechanism directly
            System.out.println("\nTesting fallback mechanism...");
            String fallbackContent = contentWithThaiPrefix;
            
            if (fallbackContent != null && fallbackContent.contains(": ")) {
                if (fallbackContent.matches("^[\\u0E00-\\u0E7F].*")) {
                    int colonIndex = fallbackContent.indexOf(": ");
                    if (colonIndex > 0) {
                        fallbackContent = fallbackContent.substring(colonIndex + 2);
                    }
                }
            }
            
            System.out.println("Fallback result: " + fallbackContent);

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}