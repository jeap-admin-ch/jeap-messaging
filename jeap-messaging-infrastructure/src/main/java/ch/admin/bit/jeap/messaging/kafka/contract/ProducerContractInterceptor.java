package ch.admin.bit.jeap.messaging.kafka.contract;

import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.model.MessageType;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;

import java.util.Map;

@Slf4j
public class ProducerContractInterceptor implements ProducerInterceptor<Object, Object> {
    public static final String CONTRACTS_VALIDATOR = "producerContractInterceptor.contractsValidator";
    public static final String ALLOW_NO_CONTRACT_EVENTS = "producerContractInterceptor.allowNoContractEvents";
    public static final String ALLOW_NO_CONTRACT_EVENTS_SILENT = "producerContractInterceptor.allowNoContractEventsSilent";
    public static final String EXEMPT_FROM_PRODUCER_CONTRACT_CHECK_HEADER = "exemptFromProducerContractCheckMarker";

    private ContractsValidator contractsValidator;
    private boolean allowNoContractEvents;
    private boolean allowNoContractEventsSilent;

    @Override
    public void configure(Map<String, ?> configs) {
        contractsValidator = (ContractsValidator) configs.get(CONTRACTS_VALIDATOR);
        allowNoContractEvents = (Boolean) configs.get(ALLOW_NO_CONTRACT_EVENTS);
        allowNoContractEventsSilent = (Boolean) configs.get(ALLOW_NO_CONTRACT_EVENTS_SILENT);
    }

    @Override
    public ProducerRecord<Object, Object> onSend(ProducerRecord<Object, Object> record) {
        //In this case we do not even need to check
        if (allowNoContractEventsSilent) {
            return record;
        }
        // No contract warnings/error for test messages, i.e. messages produced by an integration test
        if (isExemptFromProducerContractCheck(record)) {
            return record;
        }
        // Not a jEAP message - skip check
        if (!(record.value() instanceof AvroMessage avroMessage)) {
            return record;
        }

        MessageType type = avroMessage.getType();
        String topic = record.topic();
        try {
            contractsValidator.ensurePublisherContract(type, topic);
            return record;
        } catch (NoContractException e) {
            if (allowNoContractEvents) {
                String message = String.format("You have no contract to publish events of type %s on topic %s but still do so. " +
                                "However as publishWithoutContractAllowed is ON this event will still be published",
                        type, topic);
                log.warn(message);
                return record;
            }
            String message = String.format("You have no contract to publish events of type %s on topic %s but still do so. " +
                            "This event is NOT published and an exception is send to the application. " +
                            "Use publishWithoutContractAllowed to change this behavior in a dev environment",
                    type, topic);
            log.error(message, e);

            /*
             * This is kind of hacky. We cannot throw an exception here as they are not propagated (check description on
             * {@link ProducerInterceptor#onSend(ProducerRecord)}). However, we can return a record that throws an exception
             * if its evaluated
             */
            return new NoContractProducerRecord<>(e);
        }
    }

    private static boolean isExemptFromProducerContractCheck(ProducerRecord<Object, Object> producerRecord) {
        Iterable<Header> exemptHeaders = producerRecord.headers().headers(EXEMPT_FROM_PRODUCER_CONTRACT_CHECK_HEADER);
        return exemptHeaders.iterator().hasNext();
    }

    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        // nothing to do here
    }

    @Override
    public void close() {
        // nothing to do here
    }
}
