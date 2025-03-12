package com.ninja.BankStAnalysis.infra.nao.model;


import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class BulkAssociationRequest {
    @NotNull
    private String realmId;
    @NotNull
    private Integer userId; // The user who owns these associations
    @NotNull
    private List<AssociationCreationRequest> associations;
}