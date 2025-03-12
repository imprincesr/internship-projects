package com.ninja.BankStAnalysis.core.ananomyzer.iface;

import java.util.Arrays;
import java.util.List;

public enum BankStatementJSONPaths {
	ACCOUNT_XNS(Arrays.asList("$.accountXns[*].xns[*].date", "$.accountXns[*].xns[*].amount",
			"$.accountXns[*].xns[*].balance", "$.accountXns[*].xns[*].narration", "$.accountXns[*].xns[*].mox",
			"$.accountXns[*].xns[*].chqNo")),
	EOD_BALANCE(
			Arrays.asList("$.accountAnalysis[*].eODBalances[*].date", "$.accountAnalysis[*].eODBalances[*].balance"));

	private final List<String> jsonPaths;

	// Constructor
	BankStatementJSONPaths(List<String> jsonPaths) {
		this.jsonPaths = jsonPaths;
	}
	
	 public List<String> getJSONPaths() {
	        return jsonPaths;
	    }
}
