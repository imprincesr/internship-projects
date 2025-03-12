package com.ninja.BankStAnalysis.core.enums;

import java.util.Arrays;
import java.util.List;

public enum PerfiosNinjacartJSONPaths {
    BANK_TRANSACTION(Arrays.asList(
            "$.report.accountXns[*].xns[*].date",
            "$.report.accountXns[*].xns[*].amount",
            "$.report.accountXns[*].xns[*].balance"),
            "PERFIOS", "v1.0"),
    ACCOUNT_XNS(Arrays.asList(
            "$.report.accountXns[*].xns[*].date",
            "$.report.accountXns[*].xns[*].amount",
            "$.report.accountXns[*].xns[*].balance",
            "$.report.accountXns[*].xns[*].narration",
            "$.report.accountXns[*].xns[*].mox",
            "$.report.accountXns[*].xns[*].chqNo",
            "$.report.accountXns[*].xns[*].category"),
            "PERFIOS", "v1.0"),
    EOD_BALANCE(
            Arrays.asList(
                    "$.report.accountAnalysis[*].eODBalances[*].date",
                    "$.report.accountAnalysis[*].eODBalances[*].balance"),
            "PERFIOS", "v1.0");
    private final List<String> jsonPaths;
    private final String provider;
    private final String providerVersion;

    // Constructor
    PerfiosNinjacartJSONPaths(List<String> jsonPaths, String provider, String providerVersion) {
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

