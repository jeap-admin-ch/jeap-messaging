package ch.admin.bit.jeap.messaging.sequentialinbox.jpa;

import ch.admin.bit.jeap.messaging.sequentialinbox.inbox.SequenceInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
interface SpringDataJpaSequenceInstanceRepository extends JpaRepository<SequenceInstance, Long> {

    Optional<SequenceInstance> findByTypeAndContextId(String type, String contextId);

}
