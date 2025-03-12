package com.ninja.BankStAnalysis.infra.persistence;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_bank_details")
public class UserBankDetails {

    @Id
    private Long id;
    private Integer userId;

    @Column(columnDefinition = "bpchar(18)")
    private String realmId;
    private String accountNumber;
    private String bankName;
}
//CREATE TABLE user_bank_details (
// id BIGSERIAL PRIMARY KEY ,
// user_id INTEGER NOT NULL,
// realm_id CHAR(18) NOT NULL,
// account_number VARCHAR(36) NOT NULL,
// bank_name VARCHAR(255) NOT NULL
// );