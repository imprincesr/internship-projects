package com.ninja.BankStAnalysis.core.enums;

import java.util.Arrays;
import java.util.List;

public enum PerfiosJSONPaths {
    BANK_TRANSACTION(Arrays.asList(
            "$.accountXns[*].xns[*].date",
            "$.accountXns[*].xns[*].amount",
            "$.accountXns[*].xns[*].balance"),
            "PERFIOS", "v1.0"),
    ACCOUNT_XNS(Arrays.asList(
            "$.accountXns[*].xns[*].date",
            "$.accountXns[*].xns[*].amount",
            "$.accountXns[*].xns[*].balance",
            "$.accountXns[*].xns[*].narration",
            "$.accountXns[*].xns[*].mox",
            "$.accountXns[*].xns[*].chqNo",
            "$.accountXns[*].xns[*].category"),
            "PERFIOS", "v1.0"),
    EOD_BALANCE(
            Arrays.asList(
                    "$.accountAnalysis[*].eODBalances[*].date",
                    "$.accountAnalysis[*].eODBalances[*].balance"),
            "PERFIOS", "v1.0");

    private final List<String> jsonPaths;
    private final String provider;
    private final String providerVersion;

    // Constructor
    PerfiosJSONPaths(List<String> jsonPaths, String provider, String providerVersion) {
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
