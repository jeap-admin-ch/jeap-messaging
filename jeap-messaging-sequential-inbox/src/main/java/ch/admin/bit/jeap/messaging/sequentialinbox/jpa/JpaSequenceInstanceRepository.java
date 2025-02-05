package ch.admin.bit.jeap.messaging.sequentialinbox.jpa;

import ch.admin.bit.jeap.messaging.sequentialinbox.inbox.SequenceInstance;
import ch.admin.bit.jeap.messaging.sequentialinbox.inbox.SequenceInstanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
class JpaSequenceInstanceRepository implements SequenceInstanceRepository {

    private final SpringDataJpaSequenceInstanceRepository springDataJpaSequenceInstanceRepository;

    @Override
    public SequenceInstance save(SequenceInstance sequenceInstance) {
        return springDataJpaSequenceInstanceRepository.save(sequenceInstance);
    }

    @Override
    public List<SequenceInstance> findAll() {
        return springDataJpaSequenceInstanceRepository.findAll();
    }

    @Override
    public List<SequenceInstance> findByTypeAndContextId(String type, String contextId) {
        return springDataJpaSequenceInstanceRepository.findByTypeAndContextId(type, contextId);
    }

}
