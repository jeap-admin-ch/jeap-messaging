package ch.admin.bit.jeap.messaging.sequentialinbox.jpa;

import ch.admin.bit.jeap.messaging.sequentialinbox.inbox.SequenceInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
interface SpringDataJpaSequenceInstanceRepository extends JpaRepository<SequenceInstance, Long> {

    List<SequenceInstance> findByTypeAndContextId(String type, String contextId);

}
