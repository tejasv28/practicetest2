package com.bfhl.solution.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response received from the generateWebhook endpoint.
 * Contains the target webhook URL and the JWT access token.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookResponse {

    @JsonProperty("webhook")
    private String webhook;

    @JsonProperty("accessToken")
    private String accessToken;

    public WebhookResponse() {}

    public String getWebhook()                   { return webhook; }
    public void   setWebhook(String webhook)     { this.webhook = webhook; }

    public String getAccessToken()               { return accessToken; }
    public void   setAccessToken(String token)   { this.accessToken = token; }
}
