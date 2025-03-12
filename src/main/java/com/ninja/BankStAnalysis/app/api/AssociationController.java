package com.ninja.BankStAnalysis.app.api;


import com.ninja.BankStAnalysis.infra.exceptions.ErrorCode;
import com.ninja.BankStAnalysis.infra.exceptions.ServiceRuntimeException;
import com.ninja.BankStAnalysis.infra.service.AssociationService;
import com.ninja.BankStAnalysis.infra.nao.model.UserAccountAssociation;
import com.ninja.BankStAnalysis.infra.nao.model.AssociationCreationRequest;
import com.ninja.BankStAnalysis.infra.nao.model.BulkAssociationRequest;
import com.ninja.BankStAnalysis.infra.nao.model.NaoPrefix;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@Validated
@RestController
@RequestMapping("/api/v1/realms/{realmId}/users/{userId}/associations")
@RequiredArgsConstructor
public class AssociationController {
    private final AssociationService associationService;

    @PostMapping("")
    ResponseEntity<Void> createAssociation(
            @PathVariable("realmId") String realmId,
            @PathVariable("userId") Integer userId,
            @NotNull @Valid @RequestParam(value = "naoPrefix") NaoPrefix naoPrefix,
            @Valid @RequestBody AssociationCreationRequest req) {
        if (Objects.isNull(req.getSourceObjectId()) || Objects.isNull(req.getSourceObjectType())
                || Objects.isNull(req.getAssociationType()) || Objects.isNull(req.getDestinationObjectId())
                || Objects.isNull(req.getDestinationObjectType())) {
            throw new ServiceRuntimeException(ErrorCode.RS_ERROR_0001);
        }
        if (!req.getSourceObjectId().equals(userId.toString())) {
            throw new ServiceRuntimeException(ErrorCode.RS_ERROR_0002, "sourceObjectId must be the userId");
        }
        if (!"USER".equals(req.getSourceObjectType())) {
            throw new ServiceRuntimeException(ErrorCode.RS_ERROR_0001, "sourceObjectType must be 'USER'");
        }
        if (!"ACCOUNT".equals(req.getDestinationObjectType())) {
            throw new ServiceRuntimeException(ErrorCode.RS_ERROR_0001, "destinationObjectType must be 'ACCOUNT'");
        }
        associationService.createAssociation(realmId, userId, naoPrefix, req);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PostMapping("/bulk")
    public ResponseEntity<List<UserAccountAssociation>> createBulkAssociation(
            @PathVariable("realmId") String realmId,
            @PathVariable("userId") Integer userId,
            @Validated @RequestBody BulkAssociationRequest request,
            @NotNull @Valid @RequestParam(value = "naoPrefix") NaoPrefix naoPrefix) {
        if (!request.getUserId().equals(userId)) {
            throw new ServiceRuntimeException(ErrorCode.RS_ERROR_0002, "Request userId must match path userId");
        }
        for (AssociationCreationRequest req : request.getAssociations()) {
            if (!req.getSourceObjectId().equals(userId.toString())) {
                throw new ServiceRuntimeException(ErrorCode.RS_ERROR_0002, "sourceObjectId must match userId in all associations");
            }
            if (!"USER".equals(req.getSourceObjectType())) {
                throw new ServiceRuntimeException(ErrorCode.RS_ERROR_0001, "sourceObjectType must be 'USER'");
            }
            if (!"ACCOUNT".equals(req.getDestinationObjectType())) {
                throw new ServiceRuntimeException(ErrorCode.RS_ERROR_0001, "destinationObjectType must be 'ACCOUNT'");
            }
        }
        List<UserAccountAssociation> savedAssociations = associationService.createBulkAssociation(
                request.getRealmId(),
                request.getUserId(),
                naoPrefix,
                request.getAssociations()
        );
        return ResponseEntity.ok(savedAssociations);
    }
}