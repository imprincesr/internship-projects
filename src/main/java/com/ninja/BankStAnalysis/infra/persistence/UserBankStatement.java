package com.ninja.BankStAnalysis.infra.persistence;

import com.ninja.BankStAnalysis.core.modelHelper.BankStatementHashType;
import com.ninja.BankStAnalysis.core.modelHelper.Provider;
import com.ninja.BankStAnalysis.core.modelHelper.SourceType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_bank_statement",
        indexes = {
                @Index(name = "idx_user_bank_stmt_user_id", columnList = "user_id"),
                @Index(name = "idx_user_bank_stmt_account_number", columnList = "account_number"),
                @Index(name = "idx_user_bank_stmt_root_hash", columnList = "root_hash, hash_type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserBankStatement implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unique primary key for table

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "realm_id", nullable = false, columnDefinition = "bpchar(18)")
    private String realmId;

    @Column(name = "account_number", nullable = false, length = 36)
    private String accountNumber;

    @Column(name = "phone_number", nullable = false, length = 18)
    private String phoneNumber;

    @Column(name = "root_hash", nullable = false, length = 64)
    private String rootHash;

    @Column(name = "hash_type", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private BankStatementHashType hashType;

    @Column(name = "provider", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private Provider provider;

    @Column(name = "source_type", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private SourceType sourceType;

    @Column(name = "media_link", columnDefinition = "TEXT")
    private String mediaLink;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "modified_by")
    private String modifiedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "modified_at")
    private LocalDateTime modifiedAt;
}

// CREATE SEQUENCE user_bank_statement_seq START WITH 1 INCREMENT BY 1000;
//-- Creating user_bank_statement table
//CREATE TABLE user_bank_statement (
//id BIGSERIAL PRIMARY KEY,
//user_id INTEGER NOT NULL,
//realm_id CHAR(18) NOT NULL,
//account_number VARCHAR(36) NOT NULL,
//phone_number VARCHAR(18) NOT NULL,
//root_hash VARCHAR(64) NOT NULL,
//hash_type SMALLINT NOT NULL,
//provider SMALLINT NOT NULL,
//source_type SMALLINT NOT NULL,
//media_link TEXT,
//created_by VARCHAR(255) NOT NULL,
//modified_by VARCHAR(255),
//created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
//modified_at TIMESTAMP
//);
//
//        -- Creating indexes for faster queries
//CREATE INDEX idx_user_bank_stmt_user_id ON user_bank_statement(user_id);
//CREATE INDEX idx_user_bank_stmt_account_number ON user_bank_statement(account_number);
//CREATE INDEX idx_user_bank_stmt_root_hash ON user_bank_statement(root_hash, hash_type);
//
