package com.bfhl.solution.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload submitted to the testWebhook endpoint containing the final SQL answer.
 */
public class QuerySubmission {

    @JsonProperty("finalQuery")
    private String finalQuery;

    public QuerySubmission() {}

    public QuerySubmission(String finalQuery) {
        this.finalQuery = finalQuery;
    }

    public String getFinalQuery()                    { return finalQuery; }
    public void   setFinalQuery(String finalQuery)   { this.finalQuery = finalQuery; }
}
