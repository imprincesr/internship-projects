package com.ninja.BankStAnalysis.core.ananomyzer.iface;

public class AnonymousToken {
	private final int userId;
	private final String realmId;
	private final String accountId;
	private final boolean primaryOwner;
	private String token;
	private boolean combinedToken;

	public AnonymousToken(int userId, String realmId, String accountId, boolean isPrimaryOwner) {
		this.userId = userId;
		this.realmId = realmId;
		this.accountId = accountId;
		this.primaryOwner = isPrimaryOwner;
		this.combinedToken = false;
	}

	public int getUserId() {
		return userId;
	}

	public String getRealmId() {
		return realmId;
	}

	public String getAccountId() {
		return accountId;
	}

	public boolean isPrimaryOwner() {
		return primaryOwner;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String toString() {
		return String.format("(%d - %s - %s - %s - %s - [%s]", userId, realmId, accountId, primaryOwner ? "TRUE" : "FALSE",
				token, combinedToken ? "Combined TXN" : "Individual TXN");
	}

	public boolean isCombinedToken() {
		return combinedToken;
	}

	public void setCombinedToken(boolean combinedToken) {
		this.combinedToken = combinedToken;
	}
}
