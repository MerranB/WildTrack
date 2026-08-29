package com.wildtrack.repository;

import com.wildtrack.client.dto.MovebankEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MovebankEventBatchWriter {

    private static final String INSERT_SQL = """
              INSERT INTO movebank_event (timestamp, location, individual_id, tag_id)
              VALUES (?, ST_SetSRID(ST_MakePoint(?, ?), 4326), ?, ?)
              ON CONFLICT ON CONSTRAINT unique_data_point DO NOTHING
              """;

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public int insertBatch(List<MovebankEventDto> rows) {
        if (rows.isEmpty()) {
            return 0;
        }

        int[] results = jdbcTemplate.batchUpdate(INSERT_SQL, new BatchPreparedStatementSetter() {

            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                MovebankEventDto dto = rows.get(i);
                ps.setTimestamp(1, Timestamp.valueOf(dto.getTimestamp()));
                ps.setDouble(2, dto.getLocationLong());
                ps.setDouble(3, dto.getLocationLat());
                ps.setString(4, dto.getIndividualId());
                ps.setString(5, dto.getTagId());
            }

            @Override
            public int getBatchSize() {
                return rows.size();
            }
        });

        int inserted = 0;
        for (int result : results) {
            if (result > 0) {
                inserted += result;
            }
        }
        return inserted;
    }
}