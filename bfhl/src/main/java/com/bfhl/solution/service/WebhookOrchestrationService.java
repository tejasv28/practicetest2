package com.bfhl.solution.service;

import com.bfhl.solution.dto.QuerySubmission;
import com.bfhl.solution.dto.WebhookRequest;
import com.bfhl.solution.dto.WebhookResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Runs automatically once the Spring context is fully loaded (ApplicationRunner).
 * No HTTP endpoint is exposed — all communication is outbound-only.
 *
 * Flow:
 *  1. POST to generateWebhook → receive webhook URL + accessToken
 *  2. Determine correct SQL question from regNo
 *  3. POST final SQL query to the webhook URL with Bearer token
 */
@Service
public class WebhookOrchestrationService implements ApplicationRunner {

    // ── Constants ─────────────────────────────────────────────────────────────

    private static final String GENERATE_WEBHOOK_URL =
            "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

    // Candidate details — replace these with your actual values before submission
    private static final String CANDIDATE_NAME  = "John Doe";
    private static final String CANDIDATE_REG   = "REG12347";
    private static final String CANDIDATE_EMAIL = "john@example.com";

    // Maximum retry attempts when the submission call fails transiently
    private static final int MAX_RETRIES = 3;

    // ── Dependencies ──────────────────────────────────────────────────────────

    private final RestTemplate        restTemplate;
    private final SqlResolverService  sqlResolverService;

    @Autowired
    public WebhookOrchestrationService(RestTemplate restTemplate,
                                       SqlResolverService sqlResolverService) {
        this.restTemplate       = restTemplate;
        this.sqlResolverService = sqlResolverService;
    }

    // ── ApplicationRunner entry point ─────────────────────────────────────────

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("=== BFHL Qualifier — startup sequence initiated ===");

        try {
            // Step 1: generate the webhook and obtain credentials
            WebhookResponse credentials = callGenerateWebhook();

            System.out.println("[Orchestration] Webhook URL   : " + credentials.getWebhook());
            System.out.println("[Orchestration] AccessToken   : " + credentials.getAccessToken());

            // Step 2: pick the right SQL query
            String sqlQuery = sqlResolverService.resolveQuery(CANDIDATE_REG);
            System.out.println("[Orchestration] SQL selected  :\n" + sqlQuery);

            // Step 3: submit the answer (with retries)
            submitAnswer(credentials.getWebhook(), credentials.getAccessToken(), sqlQuery);

        } catch (Exception ex) {
            System.err.println("[Orchestration] Fatal error during startup flow: "
                    + ex.getMessage());
            ex.printStackTrace();
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Calls the generateWebhook endpoint and returns the parsed response.
     */
    private WebhookResponse callGenerateWebhook() {
        WebhookRequest body = new WebhookRequest(
                CANDIDATE_NAME, CANDIDATE_REG, CANDIDATE_EMAIL);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<WebhookRequest> request = new HttpEntity<>(body, headers);

        System.out.println("[Orchestration] Calling generateWebhook ...");
        ResponseEntity<WebhookResponse> response = restTemplate.postForEntity(
                GENERATE_WEBHOOK_URL, request, WebhookResponse.class);

        if (response.getBody() == null) {
            throw new RuntimeException("generateWebhook returned an empty body.");
        }
        return response.getBody();
    }

    /**
     * Posts the SQL query to the webhook URL.
     * Retries up to MAX_RETRIES times on failure with a short back-off.
     */
    private void submitAnswer(String webhookUrl, String accessToken, String sqlQuery) {
        QuerySubmission payload = new QuerySubmission(sqlQuery);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // The access token is used directly as a JWT Bearer token
        headers.set(HttpHeaders.AUTHORIZATION, accessToken);

        HttpEntity<QuerySubmission> request = new HttpEntity<>(payload, headers);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                System.out.println("[Orchestration] Submitting answer (attempt "
                        + attempt + "/" + MAX_RETRIES + ") ...");

                ResponseEntity<String> response = restTemplate.postForEntity(
                        webhookUrl, request, String.class);

                System.out.println("[Orchestration] Submission HTTP status : "
                        + response.getStatusCode());
                System.out.println("[Orchestration] Submission response    : "
                        + response.getBody());
                System.out.println("=== BFHL Qualifier — completed successfully ===");
                return; // exit on success

            } catch (Exception ex) {
                System.err.println("[Orchestration] Attempt " + attempt
                        + " failed: " + ex.getMessage());

                if (attempt < MAX_RETRIES) {
                    sleepQuietly(2000L * attempt); // incremental back-off
                } else {
                    throw new RuntimeException(
                            "All " + MAX_RETRIES + " submission attempts failed.", ex);
                }
            }
        }
    }

    /** Non-disruptive sleep helper. */
    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
