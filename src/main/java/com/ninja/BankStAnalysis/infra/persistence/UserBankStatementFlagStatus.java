package com.ninja.BankStAnalysis.infra.persistence;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(
        name = "user_bank_statement_flag_status",
        indexes = {
                @Index(name = "idx_user_bank_stmt_flag_user_id", columnList = "user_id"),
                @Index(name = "idx_user_bank_stmt_flag_account_number", columnList = "account_number")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserBankStatementFlagStatus implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unique primary key

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "realm_id", nullable = false, columnDefinition = "bpchar(36)")
    private String realmId;

    @Column(name = "flag", nullable = false, length = 16)
    private String flag;

    @Column(name = "flagged_on")
    private LocalDateTime flaggedOn;

    @Column(name = "account_number", nullable = false, length = 36)
    private String accountNumber;

    @Column(name = "matched_counter_party_user_ids", columnDefinition = "integer[]")
    private List<Integer> matchedCounterPartyUserIds;

    @Column(name = "matched_counter_party_account_nos", columnDefinition = "varchar(36)[]")
    private List<String> matchedCounterPartyAccountNos;

    @CreationTimestamp
    @Column(name = "verified_on", nullable = false)
    private LocalDateTime verifiedOn;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = true)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", nullable = true)
    private String updatedBy;
}

//CREATE TABLE user_bank_statement_flag_status (
//id BIGSERIAL PRIMARY KEY,  -- Auto-incrementing primary key
//user_id INTEGER NOT NULL,
//realm_id CHAR(36) NOT NULL,
//flag VARCHAR(16) NOT NULL,
//flagged_on TIMESTAMP,
//account_number VARCHAR(36) NOT NULL,
//matched_counter_party_user_ids INTEGER[], -- Array of integers
//matched_counter_party_account_nos VARCHAR(36)[], -- Array of varchar(36)
//verified_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
//created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
//created_by VARCHAR(255) NOT NULL,
//updated_at TIMESTAMP DEFAULT NULL,
//updated_by VARCHAR(255) DEFAULT NULL
//);
//
//        -- Indexes for faster lookups
//CREATE INDEX idx_user_bank_stmt_flag_user_id ON user_bank_statement_flag_status (user_id);
//CREATE INDEX idx_user_bank_stmt_flag_account_number ON user_bank_statement_flag_status (account_number);

//CONSTRAINT uq_user_realm_account UNIQUE (user_id, realm_id, account_number)