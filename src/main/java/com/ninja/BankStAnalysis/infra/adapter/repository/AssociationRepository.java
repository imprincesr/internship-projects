package com.ninja.BankStAnalysis.infra.adapter.repository;


import com.ninja.BankStAnalysis.infra.nao.model.UserAccountAssociation;
import jakarta.transaction.Transactional;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;

@Repository
public class AssociationRepository {

    private final JdbcTemplate jdbcTemplate;

    public AssociationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private void createAssociationAfterFetch(Integer userId, String realmId, String accountNumber) {
        UserAccountAssociation association = new UserAccountAssociation();
        association.setRealmId(realmId);
        association.setUserId(userId);
        association.setSourceObjectId(userId.toString()); // userId as String
        association.setSourceObjectType("USER");
        association.setDestinationObjectId(accountNumber); // accountNumber
        association.setDestinationObjectType("ACCOUNT");
        association.setAssociationType("MANAGES");
        association.setIsPrimary(false);
        association.setCreatedBy("SYSTEM");
        association.setCreatedAt(java.time.LocalDateTime.now());
        saveAssociation(association);
    }

    @Transactional
    public void saveAssociation(UserAccountAssociation association) {
        List<UserAccountAssociation> associations = Collections.singletonList(association);
        saveBulkAssociations(associations);
    }

    @Transactional
    public void saveBulkAssociations(List<UserAccountAssociation> associations) {
        String sql = """
            INSERT INTO user_account_association 
            (realm_id, user_id, source_object_id, source_object_type, destination_object_id, destination_object_type, association_type, is_primary, created_by, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (realm_id, user_id, source_object_id, destination_object_id)
            DO UPDATE SET 
                source_object_type = EXCLUDED.source_object_type,
                destination_object_type = EXCLUDED.destination_object_type,
                association_type = EXCLUDED.association_type,
                is_primary = EXCLUDED.is_primary,
                created_by = EXCLUDED.created_by,
                created_at = EXCLUDED.created_at
        """;

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws java.sql.SQLException {
                UserAccountAssociation association = associations.get(i);
                ps.setString(1, association.getRealmId());
                ps.setInt(2, association.getUserId());
                ps.setString(3, association.getSourceObjectId()); // userId as String
                ps.setString(4, association.getSourceObjectType()); // "USER"
                ps.setString(5, association.getDestinationObjectId()); // accountNumber
                ps.setString(6, association.getDestinationObjectType()); // "ACCOUNT"
                ps.setString(7, association.getAssociationType());
                ps.setBoolean(8, association.getIsPrimary());
                ps.setString(9, association.getCreatedBy());
                ps.setTimestamp(10, Timestamp.valueOf(association.getCreatedAt()));
            }

            @Override
            public int getBatchSize() {
                return associations.size();
            }
        });
    }
}
