package ch.admin.bit.jeap.messaging.kafka.reactive.test;

import ch.admin.bit.jeap.messaging.kafka.contract.ConsumerContractInterceptor;
import reactor.kafka.receiver.ReceiverOptions;

public class TestReceiverOptions {

    /**
     * Configure the given consumer options to exempt sent messages from consumer contract checking.
     *
     * @param receiverOptions the options to set the test option on
     * @return Consumer options that exempt sent messages from consumer contract checking.
     * @param <K> message key
     * @param <V> message value
     */
    public static <K,V> ReceiverOptions<K,V> from(ReceiverOptions<K,V> receiverOptions) {
        return receiverOptions.consumerProperty(ConsumerContractInterceptor.EXEMPT_FROM_CONSUMER_CONTRACT_CHECK, "true");
    }

}
