package com.ninja.BankStAnalysis.core.enums;

import java.util.Arrays;
import java.util.List;

public enum FinboxJSONPaths {
    BANK_TRANSACTION(Arrays.asList(
            "$.accounts[%d].data.transactions[*].date",
            "$.accounts[%d].data.transactions[*].amount",
            "$.accounts[%d].data.transactions[*].balance"),
            "Finbox", "v1.0"),

    DATA_TRANSACTION(Arrays.asList(
            "$.accounts[%d].data.transactions[*].date",
            "$.accounts[%d].data.transactions[*].amount",
            "$.accounts[%d].data.transactions[*].transaction_note",
            "$.accounts[%d].data.transactions[*].transaction_type",
            "$.accounts[%d].data.transactions[*].metadata.transaction_channel",
            "$.accounts[%d].data.transactions[*].hash",
            "$.accounts[%d].data.transactions[*].balance"),
            "Finbox", "v1.0"),

    EOD_BALANCES(Arrays.asList(
            "$.accounts[%d].data.eod_balances[*].*"),
            "Finbox", "v1.0");

    private final List<String> jsonPaths;
    private final String provider;
    private final String providerVersion;

    // Constructor
    FinboxJSONPaths(List<String> jsonPaths, String provider, String providerVersion) {
        this.jsonPaths = jsonPaths;
        this.provider = provider;
        this.providerVersion = providerVersion;
    }

    public List<String> getJSONPaths() {
        return jsonPaths;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderVersion() {
        return providerVersion;
    }
}
