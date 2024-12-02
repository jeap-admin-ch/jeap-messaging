package ch.admin.bit.jeap.messaging.kafka.contract;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;

/**
 * A {@link ProducerRecord} to return wrapping an exception. Any method call will simply throw the wrapped exception.
 */
class NoContractProducerRecord<K, V> extends ProducerRecord<K, V> {
    private final NoContractException noContractException;

    NoContractProducerRecord(NoContractException noContractException) {
        super("none", null);
        this.noContractException = noContractException;
    }

    @Override
    public V value() {
        throw noContractException;
    }

    @Override
    public K key() {
        throw noContractException;
    }

    @Override
    public String topic() {
        throw noContractException;
    }

    @Override
    public Long timestamp() {
        throw noContractException;
    }

    @Override
    public Headers headers() {
        throw noContractException;
    }
}
