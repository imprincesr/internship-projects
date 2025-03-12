package com.ninja.BankStAnalysis.infra.persistence;

import com.ninja.BankStAnalysis.core.modelHelper.BankStatementHashType;
import com.ninja.BankStAnalysis.core.modelHelper.Provider;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_bank_transaction",
        indexes = {
                @Index(name = "idx_user_bank_stmt_user_id", columnList = "user_id"),
                @Index(name = "idx_user_bank_stmt_account_number", columnList = "account_number"),
                @Index(name = "idx_user_bank_stmt_hash", columnList = "hash, hash_type"),
                @Index(name = "idx_user_bank_txn_statement_id", columnList = "statement_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserBankTransaction implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // Unique primary key for table

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "realm_id", nullable = false, columnDefinition = "bpchar(18)")
    private String realmId;

    @Column(name = "account_number", nullable = false, length = 36)
    private String accountNumber;

    @Column(name = "hash", nullable = false, length = 64)
    private String hash;

    @Column(name = "hash_type", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private BankStatementHashType hashType;

    @Column(name = "provider", nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private Provider provider;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "statement_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transaction_statement_id", value = ConstraintMode.CONSTRAINT))
    private UserBankStatement statementId; // Foreign key reference
}

//-- Create a shared sequence
//CREATE SEQUENCE user_bank_transaction_id_seq;

//-- Creating partitioned user_bank_transaction table
//CREATE TABLE user_bank_transaction (
//id BIGSERIAL NOT NULL,
//user_id INTEGER NOT NULL,
//realm_id CHAR(18) NOT NULL,
//account_number VARCHAR(36) NOT NULL,
//hash VARCHAR(64) NOT NULL,
//hash_type SMALLINT NOT NULL,
//provider SMALLINT NOT NULL,
//statement_id BIGINT NOT NULL,  -- Foreign Key reference to user_bank_statement(id)
//created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
//PRIMARY KEY (id, hash_type)
//) PARTITION BY LIST (hash_type);
//
//-- Creating partitions
//CREATE TABLE bank_transaction PARTITION OF user_bank_transaction FOR VALUES IN (0);
//CREATE TABLE account_xns PARTITION OF user_bank_transaction FOR VALUES IN (1);
//CREATE TABLE eod_balance PARTITION OF user_bank_transaction FOR VALUES IN (2);
//
//-- Adding foreign key constraint for cascading delete
//ALTER TABLE user_bank_transaction
//ADD CONSTRAINT fk_transaction_statement_id
//FOREIGN KEY (statement_id) REFERENCES user_bank_statement(id)
//ON DELETE CASCADE;
//
//-- Adding index on statement_id for faster lookups
//CREATE INDEX idx_user_bank_txn_statement_id ON user_bank_transaction(statement_id);
