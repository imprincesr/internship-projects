package com.ninja.BankStAnalysis.core.enums;

import java.util.Arrays;
import java.util.List;

public enum OneMoneyJSONPaths {
	BANK_TRANSACTION(Arrays.asList(
			"$.data[%d].Transactions.Transaction[*].valueDate",
			"$.data[%d].Transactions.Transaction[*].amount",
			"$.data[%d].Transactions.Transaction[*].currentBalance"), 
			"OneMoney", "v1.0"),
	
	BANK_TRANSACTIONS_TRANSACTION(Arrays.asList(
			"$.data[%d].Transactions.Transaction[*].amount",
			"$.data[%d].Transactions.Transaction[*].currentBalance", 
			"$.data[%d].Transactions.Transaction[*].mode",
			"$.data[%d].Transactions.Transaction[*].narration",
			"$.data[%d].Transactions.Transaction[*].reference",
			"$.data[%d].Transactions.Transaction[*].transactionTimestamp",
			"$.data[%d].Transactions.Transaction[*].txnId",
			"$.data[%d].Transactions.Transaction[*].type",
			"$.data[%d].Transactions.Transaction[*].valueDate"), 
			"OneMoney", "v1.0"),
	
	SUMMARY(Arrays.asList(
			"$.data[*].Summary.balanceDateTime",
			"$.data[*].Summary.branch",
			"$.data[*].Summary.currency",
			"$.data[*].Summary.currentBalance",
			"$.data[*].Summary.currentODLimit",
			"$.data[*].Summary.drawingLimit",
			"$.data[*].Summary.exchgeRate",
			"$.data[*].Summary.facility",
			"$.data[*].Summary.ifscCode",
			"$.data[*].Summary.micrCode",
			"$.data[*].Summary.openingDate",
			"$.data[*].Summary.status",
			"$.data[*].Summary.type",
			"$.data[*].Summary.Pending.amount"), 
			"OneMoney", "v1.0");

	private final List<String> jsonPaths;
	private final String provider;
	private final String providerVersion;

	// Constructor
	OneMoneyJSONPaths(List<String> jsonPaths, String provider, String providerVersion) {
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
