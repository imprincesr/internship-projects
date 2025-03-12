package com.ninja.BankStAnalysis.core.modelHelper;

public enum DedupeStatus {
    RED(
            "RED",
            "Exact Duplicate",
            "High",
            "Exact duplicate found with same amount, date & counterparty.",
            "Further Investigation Required",
            50,
            Double.MAX_VALUE),
    AMBER(
            "AMBER",
            "Partial Match",
            "Medium",
            "Partial match found with similar amount or date.",
            "Review Suggested",
            0.01,
            50),
    GREEN(
            "GREEN",
            "No Significant Match",
            "Low",
            "No significant match detected.",
            "No Further Action Required",
            0,
            0.01);

    private final String status;
    private final String matchType;
    private final String riskLevel;
    private final String reasonForFlagging;
    private final String recommendation;
    private final double minPercentage;
    private final double maxPercentage;

    DedupeStatus(String status, String matchType, String riskLevel, String reasonForFlagging,
                 String recommendation, double minPercentage, double maxPercentage) {
        this.status = status;
        this.matchType = matchType;
        this.riskLevel = riskLevel;
        this.reasonForFlagging = reasonForFlagging;
        this.recommendation = recommendation;
        this.minPercentage = minPercentage;
        this.maxPercentage = maxPercentage;
    }

    public static DedupeStatus fromMatchPercentage(double matchPercentage) {
        for (DedupeStatus status : values()) {
            if (matchPercentage >= status.minPercentage && matchPercentage < status.maxPercentage) {
                return status;
            }
        }
        return GREEN; // Default fallback
    }

    public String getStatus() { return status; }
    public String getMatchType() { return matchType; }
    public String getRiskLevel() { return riskLevel; }
    public String getReasonForFlagging() { return reasonForFlagging; }
    public String getRecommendation() { return recommendation; }
}