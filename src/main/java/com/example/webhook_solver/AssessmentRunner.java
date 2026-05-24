package com.example.webhook_solver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class AssessmentRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(AssessmentRunner.class);

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting Assessment Flow...");
        
        RestTemplate restTemplate = new RestTemplate();

        // Step 1: Generate Webhook
        String generateWebhookUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", "John Doe");
        requestBody.put("regNo", "REG12347");
        requestBody.put("email", "john@example.com");
        
        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);
        
        logger.info("Sending POST request to generate webhook...");
        ResponseEntity<Map> response = restTemplate.postForEntity(generateWebhookUrl, requestEntity, Map.class);
        
        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            Map<String, Object> responseBody = response.getBody();
            logger.info("Received response: {}", responseBody);
            
            String webhook = (String) responseBody.getOrDefault("webhook", "https://bfhldevapigw.healthrx.co.in/hiring/testWebhook/JAVA");
            String accessToken = (String) responseBody.get("accessToken");
            
            if (accessToken == null) {
                logger.error("No access token found in response.");
                return;
            }
            
            // Step 2: Solve the SQL Problem
            // The regNo is REG12347. The last two digits are 47 (Odd).
            // Odd -> Question 1.
            String sqlQuery = "SELECT p.AMOUNT AS SALARY, CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME, " +
                              "FLOOR(DATEDIFF(CURRENT_DATE, e.DOB) / 365.25) AS AGE, d.DEPARTMENT_NAME " +
                              "FROM PAYMENTS p " +
                              "JOIN EMPLOYEE e ON p.EMP_ID = e.EMP_ID " +
                              "JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID " +
                              "WHERE DAY(p.PAYMENT_TIME) != 1 " +
                              "ORDER BY p.AMOUNT DESC LIMIT 1;";
                              
            logger.info("Prepared SQL Query: {}", sqlQuery);
            
            // Step 3: Submit the Solution
            HttpHeaders submitHeaders = new HttpHeaders();
            submitHeaders.setContentType(MediaType.APPLICATION_JSON);

            
            // Set exactly to the access token as requested: Authorization: <accessToken>
            submitHeaders.set("Authorization", accessToken);

            
            Map<String, String> submitBody = new HashMap<>();
            submitBody.put("finalQuery", sqlQuery);
            
            HttpEntity<Map<String, String>> submitEntity = new HttpEntity<>(submitBody, submitHeaders);
            
            logger.info("Submitting solution to webhook URL: {}", webhook);
            
            try {
                ResponseEntity<String> submitResponse = restTemplate.postForEntity(webhook, submitEntity, String.class);
                logger.info("Submission Response Code: {}", submitResponse.getStatusCode());
                logger.info("Submission Response Body: {}", submitResponse.getBody());
            } catch (Exception e) {
                logger.error("Error submitting solution", e);
            }
            
        } else {
            logger.error("Failed to generate webhook. Status: {}", response.getStatusCode());
        }
    }
}
