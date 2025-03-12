package com.ninja.BankStAnalysis.infra.service;

import com.ninja.BankStAnalysis.infra.nao.model.UserAccountAssociation;
import com.ninja.BankStAnalysis.infra.nao.model.AssociationCreationRequest;
import com.ninja.BankStAnalysis.infra.nao.model.BulkAssociationRequest;
import com.ninja.BankStAnalysis.infra.nao.model.NaoPrefix;
import com.ninja.BankStAnalysis.infra.adapter.repository.AssociationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssociationService {

    private final AssociationRepository associationRepository;

    public void createAssociation(String realmId, Integer userId, NaoPrefix naoPrefix, AssociationCreationRequest req) {
        UserAccountAssociation association = new UserAccountAssociation();
        association.setRealmId(realmId);
        association.setUserId(userId);
        association.setSourceObjectId(req.getSourceObjectId()); // userId as String
        association.setSourceObjectType(req.getSourceObjectType()); // "USER"
        association.setDestinationObjectId(req.getDestinationObjectId()); // accountNumber
        association.setDestinationObjectType(req.getDestinationObjectType()); // "ACCOUNT"
        association.setAssociationType(req.getAssociationType()); // e.g., "MANAGES"
        association.setIsPrimary(req.getIsPrimary() != null ? req.getIsPrimary() : false);
        association.setCreatedBy("SYSTEM");
        association.setCreatedAt(LocalDateTime.now());
        associationRepository.saveAssociation(association);
    }

    public List<UserAccountAssociation> createBulkAssociation(String realmId, Integer userId, NaoPrefix naoPrefix,
                                                              List<AssociationCreationRequest> requests) {
        List<UserAccountAssociation> associations = requests.stream().map(req -> {
            UserAccountAssociation association = new UserAccountAssociation();
            association.setRealmId(realmId);
            association.setUserId(userId);
            association.setSourceObjectId(req.getSourceObjectId()); // userId as String
            association.setSourceObjectType(req.getSourceObjectType()); // "USER"
            association.setDestinationObjectId(req.getDestinationObjectId()); // accountNumber
            association.setDestinationObjectType(req.getDestinationObjectType()); // "ACCOUNT"
            association.setAssociationType(req.getAssociationType()); // e.g., "MANAGES"
            association.setIsPrimary(req.getIsPrimary() != null ? req.getIsPrimary() : false);
            association.setCreatedBy("SYSTEM");
            association.setCreatedAt(LocalDateTime.now());
            return association;
        }).collect(Collectors.toList());

        associationRepository.saveBulkAssociations(associations);
        return associations;
    }


    public void createAssociationsFromResponse(Integer userId, String realmId, Map<String, Object> dedupeResult) {
        log.info("Starting association creation from response for userId: {}, realmId: {}", userId, realmId);
        List<AssociationCreationRequest> associations = new ArrayList<>();

        List<Map<String, Object>> transactions = (List<Map<String, Object>>) dedupeResult.get("transactions");
        if (transactions != null && !transactions.isEmpty()) {
            log.info("Found {} transactions in response", transactions.size());
            for (Map<String, Object> transaction : transactions) {
                List<Map<String, Object>> matchedCounterParties = (List<Map<String, Object>>) transaction.get("matchedCounterParty");
                if (matchedCounterParties != null && !matchedCounterParties.isEmpty()) {
                    log.info("Found {} matchedCounterParty entries in transaction", matchedCounterParties.size());
                    for (Map<String, Object> counterParty : matchedCounterParties) {
                        String counterPartyAccountNumber = (String) counterParty.get("counterPartyAccountNumber");
                        if (counterPartyAccountNumber != null && !counterPartyAccountNumber.trim().isEmpty()) {
                            log.info("Creating association for counterPartyAccountNumber: {}", counterPartyAccountNumber);
                            AssociationCreationRequest req = new AssociationCreationRequest();
                            req.setSourceObjectId(userId.toString());
                            req.setSourceObjectType("USER");
                            req.setDestinationObjectId(counterPartyAccountNumber.trim());
                            req.setDestinationObjectType("ACCOUNT");
                            req.setAssociationType("MANAGES");
                            req.setIsPrimary(false);
                            associations.add(req);
                        } else {
                            log.warn("counterPartyAccountNumber is null or empty in matchedCounterParty: {}", counterParty);
                        }
                    }
                } else {
                    log.warn("No matchedCounterParty found in transaction: {}", transaction);
                }
            }
        } else {
            log.warn("No transactions found in dedupe result");
        }

        if (!associations.isEmpty()) {
            log.info("Creating {} associations", associations.size());
            BulkAssociationRequest bulkRequest = new BulkAssociationRequest();
            bulkRequest.setRealmId(realmId);
            bulkRequest.setUserId(userId);
            bulkRequest.setAssociations(associations);
            createBulkAssociation(realmId, userId, NaoPrefix.ACCOUNT, associations);
            log.info("Associations created successfully");
        } else {
            log.warn("No associations to create from response");
        }
    }
}