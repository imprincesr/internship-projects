package com.ninja.BankStAnalysis.infra.nao.model;

import jakarta.validation.constraints.NotNull;
import lombok.Data;


@Data
public class AssociationCreationRequest {
    @NotNull
    private String sourceObjectId; // Must be userId (as String)
    @NotNull
    private String sourceObjectType; // Must be "USER"
    @NotNull
    private String destinationObjectId; // Must be accountNumber
    @NotNull
    private String destinationObjectType; // Must be "ACCOUNT"
    @NotNull
    private String associationType; // e.g., "MANAGES"
    private Boolean isPrimary; // Optional, defaults to false
}
