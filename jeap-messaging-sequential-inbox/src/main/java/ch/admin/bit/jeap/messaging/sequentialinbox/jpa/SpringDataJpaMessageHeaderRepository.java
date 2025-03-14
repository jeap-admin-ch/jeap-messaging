package ch.admin.bit.jeap.messaging.sequentialinbox.jpa;

import ch.admin.bit.jeap.messaging.sequentialinbox.persistence.MessageHeader;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;

@Repository
interface SpringDataJpaMessageHeaderRepository extends JpaRepository<MessageHeader, Long> {

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM message_header WHERE buffered_message_id IN (" +
            "SELECT bm.id FROM buffered_message bm join sequence_instance si on bm.sequence_instance_id = si.id WHERE si.retain_until < ?1)")
    void deleteExpired(ZonedDateTime cutoffTime);

    @Modifying
    @Query(nativeQuery = true, value = "DELETE FROM message_header WHERE buffered_message_id IN (" +
            "SELECT bm.id FROM buffered_message bm join sequence_instance si on bm.sequence_instance_id = si.id WHERE si.state = 'CLOSED')")
    void deleteForClosedSequences();

}
