package com.example.webhook_solver.dto;

public class QuerySubmission {
    private String finalQuery;

    public QuerySubmission(String finalQuery) {
        this.finalQuery = finalQuery;
    }

    public String getFinalQuery() {
        return finalQuery;
    }

    public void setFinalQuery(String finalQuery) {
        this.finalQuery = finalQuery;
    }
}
