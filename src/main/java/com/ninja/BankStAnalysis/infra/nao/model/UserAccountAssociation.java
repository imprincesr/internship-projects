package com.ninja.BankStAnalysis.infra.nao.model;

import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.transaction.Transactional;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Table(name = "user_account_association")
public class UserAccountAssociation {

    private Long id;
    private String realmId;
    private Integer userId; // The user who owns this association
    private String sourceObjectId; // Represents userId (as String)
    private String sourceObjectType; // Always "USER"
    private String destinationObjectId; // Represents accountNumber
    private String destinationObjectType; // Always "ACCOUNT"
    private String associationType; // e.g., "MANAGES"
    private Boolean isPrimary; // Indicates if this is the primary account
    private String createdBy;
    private LocalDateTime createdAt;
}

//
//CREATE TABLE user_account_association (
//        id BIGSERIAL PRIMARY KEY,
//        realm_id VARCHAR(255) NOT NULL,
//user_id INTEGER NOT NULL,
//source_object_id VARCHAR(255) NOT NULL, -- Stores userId as String
//source_object_type VARCHAR(50) NOT NULL, -- "USER"
//destination_object_id VARCHAR(255) NOT NULL, -- Stores accountNumber
//destination_object_type VARCHAR(50) NOT NULL, -- "ACCOUNT"
//association_type VARCHAR(50) NOT NULL, -- e.g., "MANAGES"
//is_primary BOOLEAN NOT NULL DEFAULT FALSE,
//created_by VARCHAR(50) NOT NULL,
//created_at TIMESTAMP NOT NULL,
//UNIQUE (realm_id, user_id, source_object_id, destination_object_id)
//);