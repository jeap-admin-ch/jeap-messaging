package ch.admin.bit.jeap.messaging.sequentialinbox.inbox;

import java.util.List;

public interface SequenceInstanceRepository {

    SequenceInstance save(SequenceInstance sequenceInstance);

    List<SequenceInstance> findAll();

    List<SequenceInstance> findByTypeAndContextId(String type, String contextId);

}
