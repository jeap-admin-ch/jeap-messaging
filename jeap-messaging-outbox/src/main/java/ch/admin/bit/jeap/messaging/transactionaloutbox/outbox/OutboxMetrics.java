package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

public interface OutboxMetrics {

    String MESSAGES_READY_TO_BE_SENT_COUNTER = "outbox_messages_ready_to_be_sent_count";
    String MESSAGES_FAILED_COUNTER = "outbox_messages_failed_count";
    String MESSAGES_POST_COUNTER = "outbox_messages_post_total";
    String MESSAGES_TRANSMIT_TIMER = "outbox_messages_transmit";
    String MESSAGE_DELIVERY_TYPE_TAG = "delivery_type";
    String MESSAGE_DELIVERY_TYPE_IMMEDIATE = "immediate";
    String MESSAGE_DELIVERY_TYPE_SCHEDULED = "scheduled";
    String MESSAGE_TX_STATUS_TAG = "tx_status";
    String MESSAGE_TX_STATUS_COMMITTED = "committed";
    String MESSAGE_TX_STATUS_ROLLED_BACK = "rolled_back";
    String MESSAGE_TX_STATUS_UNKNOWN = "unknown";
    String MESSAGE_RESEND_STATUS_TAG = "resend_status";
    String MESSAGE_RESEND_STATUS_RESEND_ENABLED = "resend_enabled";
    String MESSAGE_RESEND_STATUS_RESEND_DISABLED = "resend_disabled";
    String MESSAGES_READY_TO_BE_SENT_TIMER = "outbox_messages_ready_to_be_sent_query";

    /**
     * Count a send operation on the transactional outbox and tag it with dimensions 'delivery_type' and 'tx_status'.
     *
     * @param sendImmediately <code>true</code> if the send operation requested sending immediately after the transaction commit,
     *                        <code>false</code> otherwise.
     */
    void countTransactionalSend(boolean sendImmediately);

    /**
     * Update the gauges that reflect outbox metrics derived from persistent storage e.g. the current message relay lag or
     * the current number of failed messages.
     */
    void updateGauges();

    /**
     * Count the actual sending of messages on the messaging system.
     *
     * @param bootstrapServers   cluster bootstrap server address
     * @param topic              the topic where the message will be sent
     * @param messageType        the type of the message
     * @param messageTypeVersion the version of the type of the message
     */
    void countMessagingSend(String bootstrapServers, String topic, String messageType, String messageTypeVersion);
}
