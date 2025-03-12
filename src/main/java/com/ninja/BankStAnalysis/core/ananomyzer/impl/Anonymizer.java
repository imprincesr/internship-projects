package com.ninja.BankStAnalysis.core.ananomyzer.impl;

import com.google.common.collect.ImmutableList;
import com.jayway.jsonpath.JsonPath;

import java.util.*;

import com.ninja.BankStAnalysis.core.ananomyzer.core.JsonFlattener;
import com.ninja.BankStAnalysis.core.ananomyzer.core.PartialMerkleTree;
import com.ninja.BankStAnalysis.core.ananomyzer.iface.AbstractAlgorithm;
import com.ninja.BankStAnalysis.core.ananomyzer.iface.AnonymousToken;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.params.MainNetParams;

public class Anonymizer extends AbstractAlgorithm {
	final private boolean transactionLevel;
	private static final char seperator = '|';
	private static final ImmutableList<String> AccountXNSPaths = ImmutableList.of(
			"$.accountXns[*].xns[*].date",
			"$.accountXns[*].xns[*].amount", 
			"$.accountXns[*].xns[*].balance", 
			"$.accountXns[*].xns[*].narration",
			"$.accountXns[*].xns[*].mox", 
			"$.accountXns[*].xns[*].chqNo");
			// Fixme: "$.accountXns[*].xns[*].category" - Tweaking is dong at category. 
			// We need to check with business on the same

	public Anonymizer(int userId, String realmId) {
		super(userId, realmId);
		this.transactionLevel = false;
	}
	
	public Anonymizer(int userId, String realmId, boolean transactionLevel) {
		super(userId, realmId);
		this.transactionLevel = transactionLevel;
	}

	@Override
	public LinkedHashMap<String, AnonymousToken> keys(String... docs) {

		LinkedHashMap<String, AnonymousToken> tokens = new LinkedHashMap<>();

		for (String doc : docs) {
			List<String> individualTXNTokens = new ArrayList<>();
			
			// Step-1: Get Account Number
			List<String> accountNumbers = JsonPath.read(doc, "$.accountXns[*].accountNo");
			String accountNumber = accountNumbers.get(0);

			// Step-2: Initialize JSON Flattener to Fetch Account ANS information from Bank
			// Statement
			List<String> actualResult = new JsonFlattener("BankStatement-Amount XNS", AccountXNSPaths, seperator).flatten(doc);

			// Step-3: Iterate denormalized extracted results from bank statement
			for (String key : actualResult) {
				// Get Row which is comma separated
				List<String> elements = Arrays.asList(key.split("\\|"));
				
				String merkleRoot = genMerkleTreeToken (elements);

				// Create anonymous token
				AnonymousToken token = new AnonymousToken(getUserId(), getRealmId(), accountNumber, true);
				token.setToken(merkleRoot.toString());
				individualTXNTokens.add(merkleRoot.toString());

				tokens.put(merkleRoot.toString(), token);
			}
			
			
			// Step 4: Transaction level Key generation (Statement Level - Holistic Key)
			if (transactionLevel) {
				Collections.sort(individualTXNTokens);
				String merkleRoot = genMerkleTreeToken (individualTXNTokens);
				
				AnonymousToken token = new AnonymousToken(getUserId(), getRealmId(), accountNumber, true);
				token.setToken(merkleRoot.toString());
				token.setCombinedToken(true);
				tokens.put(merkleRoot.toString(), token);
			}
		}

		return tokens;
	}
	
	private String genMerkleTreeToken(List<String> elements) {
		// Step 3b: Generate field hash
		List<Sha256Hash> dataHashes = new ArrayList<>();
		for (String data : elements) {
			Sha256Hash hash = Sha256Hash.of(data.getBytes());
			dataHashes.add(hash);
		}

		// Step 3c: Set matchedChildBits to include all hashes
		int numHashes = dataHashes.size();
		byte[] matchedChildBits = new byte[(numHashes + 7) / 8];
		for (int i = 0; i < numHashes; i++) {
			int byteIndex = i / 8;
			int bitIndex = i % 8;
			matchedChildBits[byteIndex] |= (1 << bitIndex);
		}

		// Step 3d: Build the PartialMerkleTree
		NetworkParameters params = MainNetParams.get();
		PartialMerkleTree merkleTree = PartialMerkleTree.buildFromLeaves(params, matchedChildBits, dataHashes);

		// Step 3e: Generate root hash
		Sha256Hash merkleRoot = merkleTree.getTxnHashAndMerkleRoot(dataHashes);
		
		return merkleRoot.toString();
	}

	public boolean isTransactionLevel() {
		return transactionLevel;
	}

	public String generateToken(String jsonData) {
		byte[] hash = Sha256Hash.hash(jsonData.getBytes());
		return Base64.getEncoder().encodeToString(hash);
	}

}
