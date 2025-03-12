package com.ninja.BankStAnalysis.core.ananomyzer.iface;

import java.util.LinkedHashMap;

public abstract class AbstractAlgorithm {
	private final int userId;
	private final String realmId;

	public AbstractAlgorithm(int userId, String realmId) {
		this.userId = userId;
		this.realmId = realmId;
	}

	public abstract LinkedHashMap<String, AnonymousToken> keys(String... docs);

	public int getUserId() {
		return userId;
	}

	public String getRealmId() {
		return realmId;
	}
}
