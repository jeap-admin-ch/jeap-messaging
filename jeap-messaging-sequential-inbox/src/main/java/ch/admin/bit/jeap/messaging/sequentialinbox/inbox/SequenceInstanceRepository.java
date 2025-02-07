package ch.admin.bit.jeap.messaging.sequentialinbox.inbox;

import java.util.List;
import java.util.Optional;

public interface SequenceInstanceRepository {

    SequenceInstance save(SequenceInstance sequenceInstance);

    List<SequenceInstance> findAll();

    Optional<SequenceInstance> findByTypeAndContextId(String type, String contextId);

}
