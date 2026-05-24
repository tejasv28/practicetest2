package com.example.webhook_solver.service;

import com.example.webhook_solver.dto.QuerySubmission;
import com.example.webhook_solver.dto.WebhookResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class WebhookAssessmentService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(WebhookAssessmentService.class);

    private final RestTemplate restTemplate;

    @Autowired
    public WebhookAssessmentService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Initializing Webhook Assessment Protocol...");

        String generateWebhookUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("name", "John Doe");
        requestBody.put("regNo", "REG12347");
        requestBody.put("email", "john@example.com");

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            logger.info("Sending POST request to generate webhook...");
            ResponseEntity<WebhookResponse> response = restTemplate.postForEntity(
                    generateWebhookUrl, requestEntity, WebhookResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                WebhookResponse webhookData = response.getBody();
                
                String webhook = webhookData.getWebhook();
                String accessToken = webhookData.getAccessToken();

                logger.info("Successfully retrieved webhook URL: {}", webhook);
                
                if (accessToken == null || accessToken.isEmpty()) {
                    logger.error("Access token is missing from the response.");
                    return;
                }

                // Question 1 asks for the 2nd-highest salary using a correlated subquery
                String sqlQuery = "SELECT p1.AMOUNT AS SALARY, CONCAT(e.FIRST_NAME, ' ', e.LAST_NAME) AS NAME, " +
                                  "FLOOR(DATEDIFF(CURRENT_DATE, e.DOB) / 365.25) AS AGE, d.DEPARTMENT_NAME " +
                                  "FROM PAYMENTS p1 " +
                                  "JOIN EMPLOYEE e ON p1.EMP_ID = e.EMP_ID " +
                                  "JOIN DEPARTMENT d ON e.DEPARTMENT = d.DEPARTMENT_ID " +
                                  "WHERE 1 = (SELECT COUNT(DISTINCT p2.AMOUNT) FROM PAYMENTS p2 WHERE p2.AMOUNT > p1.AMOUNT);";

                logger.info("Executing solution submission...");
                submitSolution(webhook, accessToken, sqlQuery);
                
            } else {
                logger.error("Failed to generate webhook, status code: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("An error occurred during the assessment protocol: ", e);
        }
    }

    private void submitSolution(String webhookUrl, String accessToken, String sqlQuery) {
        HttpHeaders submitHeaders = new HttpHeaders();
        submitHeaders.setContentType(MediaType.APPLICATION_JSON);
        
        // Use exact token string in Authorization header without "Bearer " prefix to satisfy specific test constraints
        submitHeaders.set("Authorization", accessToken);

        QuerySubmission submission = new QuerySubmission(sqlQuery);
        HttpEntity<QuerySubmission> submitEntity = new HttpEntity<>(submission, submitHeaders);

        int maxRetries = 3;
        for (int i = 1; i <= maxRetries; i++) {
            try {
                ResponseEntity<String> submitResponse = restTemplate.postForEntity(webhookUrl, submitEntity, String.class);
                logger.info("Solution submitted successfully on attempt {}!", i);
                logger.info("Response Code: {}", submitResponse.getStatusCode());
                logger.info("Response Body: {}", submitResponse.getBody());
                return;
            } catch (Exception ex) {
                logger.error("Failed to submit solution (Attempt {}/{}): {}", i, maxRetries, ex.getMessage());
                if (i == maxRetries) {
                    logger.error("Max retries reached. Submission failed.");
                } else {
                    try {
                        Thread.sleep(2000 * i); // Back-off
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }
}
