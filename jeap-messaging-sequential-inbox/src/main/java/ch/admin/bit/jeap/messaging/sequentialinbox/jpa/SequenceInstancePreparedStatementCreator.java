package ch.admin.bit.jeap.messaging.sequentialinbox.jpa;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.SqlProvider;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;

/**
 * PreparedStatementCreator for inserting a new sequence instance. Sequence instances are created by the first message
 * arriving in a sequence context. There is a slight chance of a race condition where two messages are trying to create
 * a sequence instance concurrently. In this case, the first message will create the sequence instance, and the second
 * one will fail with a unique constraint violation. The second message will then retry the operation by reading the
 * existing instance. Unfortunately, the {@link org.hibernate.engine.jdbc.spi.SqlExceptionHelper} in Hibernate will
 * always log such constraint violation exception at the ERROR level. Discussions on the Hibernate issue tracker
 * about this issue have been going on for years, but no agreement/solution has been found yet. We thus avoid using
 * Hibernate for creating the sequence instance.
 */
record SequenceInstancePreparedStatementCreator(String name, String contextId, String state,
                                                ZonedDateTime createdAt, ZonedDateTime retainUntil)
        implements PreparedStatementCreator, SqlProvider, ResultSetExtractor<Long> {

    private final static String SQL = """
            INSERT INTO sequence_instance (id, name, context_id, state, created_at, retain_until)
            VALUES (nextval('sequence_instance_sequence'), ?, ?, ?, ?, ?) RETURNING id
            """;

    @Override
    public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
        PreparedStatement ps = con.prepareStatement(SQL);
        ps.setString(1, name);
        ps.setString(2, contextId);
        ps.setString(3, state);
        ps.setObject(4, createdAt.toOffsetDateTime());
        ps.setObject(5, retainUntil.toOffsetDateTime());
        return ps;
    }

    @Override
    public String getSql() {
        return SQL;
    }

    @Override
    public Long extractData(ResultSet rs) throws SQLException, DataAccessException {
        if (rs.next()) {
            return rs.getLong(1);
        }
        throw new IllegalStateException("Insert query failed to return an ID");
    }
}
