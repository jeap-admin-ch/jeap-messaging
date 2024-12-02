package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import java.time.Duration;

public interface TransactionalOutboxConfiguration {

    /**
     * Time to wait until a next poll for new messages to send after the previous poll finished sending messages.
     */
    Duration getPollDelay();

    /**
     * Maximum duration of continuously relaying message batches in a poll.
     * If there are a lot of unsent messages in the outbox (e.g. because Kafka is unreachable) it could take a long time of
     * continuous sending to clear the outbox. This could pose a problem for the scheduling of the relay tasks. Shedlock e.g.
     * defines a maximum lock lifespan. If clearing the outbox would take longer than this lifespan the lock would be revoked
     * by Shedlock which could result in more than one relay tasks running in parallel. To prevent this the relay tasks should
     * only continuously relay messages for a shorter timespan than the maximum lock lifespan. The ContinuousRelayTimeout configuration
     * tells the relay tasks after what time span they should stop sending messages and wait to be started again later according to
     * the configured scheduling poll interval.
     */
    Duration getContinuousRelayTimeout();

    /**
     * Maximum number of messages to process in one message relay transaction.
     */
    int getMessageRelayBatchSize();

    /**
     * Maximum duration to wait on Kafka to finish sending a message when sending is immediately after the transaction commit.
     * This timeout should not be too big in order to not delay the thread that put the messages into the outbox too much
     * for cases when Kafka is unavailable or very slow.
     */
    Duration getMessageSendImmediatelyTimeout();

    /**
     * Maximum duration to wait on Kafka to finish sending a message when sending is scheduled during a poll.
     * This timeout should be sufficiently large in order to allow the outbox to still successfully relay messages even
     * when Kafka would be slow in accepting messages.
     */
    Duration getMessageSendScheduledTimeout();

    /**
     * Maximum duration to wait on Kafka to start sending a message when sending is immediately after the transaction commit.
     * This timeout should be rather small in order to not unnecessarily delay the thread that put the messages into the outbox
     * for cases when Kafka is unavailable or very slow.
     */
    Duration getMessageSendImmediatelyMaxBlockTime();

    /**
     * Maximum duration to wait on Kafka to start sending a message when sending is scheduled during a poll.
     * This timeout should be sufficiently large in order to allow the outbox to still successfully relay messages even
     * when Kafka would be slow in accepting messages.
     */
    Duration getMessageSendScheduledMaxBlockTime();

    /**
     * Expected maximum duration for sending a message when sending is immediately after the transaction commit.
     */
    default Duration getMaxDurationSendImmediately() {
        return getMessageSendImmediatelyMaxBlockTime().plus(getMessageSendImmediatelyTimeout());
    }

    /**
     * Expected maximum duration for sending a message when sending is scheduled during a poll.
     */
    default Duration getMaxDurationSendScheduled() {
        return getMessageSendScheduledMaxBlockTime().plus(getMessageSendScheduledTimeout());
    }

    /**
     * Enable or disable the scheduled relaying of messages.
     */
    boolean isScheduledRelayEnabled();

    /**
     * Cron expression to schedule the house keeping tasks.
     */
    String getHouseKeepingSchedule();

    /**
     * Duration for which successfully sent messages are kept in the outbox before they get deleted by the house keeping.
     */
    Duration getSentMessageRetentionDuration();

    /**
     * Duration for which not yet successfully sent messages are kept in the outbox before they get deleted by the house keeping.
     */
    Duration getUnsentMessageRetentionDuration();

    /**
     * Interval between metrics updates.
     */
    Duration getMetricsUpdateInterval();

}

