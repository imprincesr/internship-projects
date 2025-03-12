package com.ninja.BankStAnalysis.infra.adapter.repository;

import com.jayway.jsonpath.JsonPath;
import com.ninja.BankStAnalysis.core.port.out.ResponsePersistRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ResponsePersistRepository implements ResponsePersistRepositoryPort {

    private final JdbcTemplate jdbcTemplate;

    public void insertUserBankStatementFlagStatus(List<Object[]> batchData) {
        if (batchData == null || batchData.isEmpty()) {
            log.warn("No records to insert into user_bank_statement_flag_status");
            return;
        }
        String INSERT_FLAG_STATUS_SQL = """
            INSERT INTO user_bank_statement_flag_status
            (user_id, realm_id, flag, flagged_on, account_number,
            matched_counter_party_user_ids, matched_counter_party_account_nos,
            verified_on, created_at, created_by, updated_at, updated_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try {
            int[] updateCounts = jdbcTemplate.batchUpdate(INSERT_FLAG_STATUS_SQL, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    Object[] record = batchData.get(i);
                    ps.setInt(1, (Integer) record[0]); // user_id
                    ps.setString(2, (String) record[1]); // realm_id
                    ps.setString(3, (String) record[2]); // flag
                    ps.setTimestamp(4, Timestamp.valueOf((LocalDateTime) record[3])); // flagged_on
                    ps.setString(5, (String) record[4]); // account_number
                    ps.setArray(6, ps.getConnection().createArrayOf("INTEGER", (Integer[]) record[5])); // matched_counter_party_user_ids
                    ps.setArray(7, ps.getConnection().createArrayOf("VARCHAR", (String[]) record[6])); // matched_counter_party_account_nos
                    ps.setTimestamp(8, Timestamp.valueOf((LocalDateTime) record[7])); // verified_on
                    ps.setTimestamp(9, Timestamp.valueOf((LocalDateTime) record[8])); // created_at
                    ps.setString(10, (String) record[9]); // created_by
                    ps.setTimestamp(11, record[10] != null ? Timestamp.valueOf((LocalDateTime) record[10]) : null); // updated_at
                    ps.setString(12, (String) record[11]); // updated_by
                }

                @Override
                public int getBatchSize() {
                    return batchData.size();
                }
            });

            log.debug("Batch insert completed with update counts: {}", Arrays.toString(updateCounts));

        } catch (Exception e) {
            log.error("Batch insert failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to insert flag status records", e);
        }
    }

}