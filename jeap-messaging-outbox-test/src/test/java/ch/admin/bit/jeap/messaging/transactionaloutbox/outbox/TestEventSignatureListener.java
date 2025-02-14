package ch.admin.bit.jeap.messaging.transactionaloutbox.outbox;

import ch.admin.bit.jeap.messaging.transactionaloutbox.test.TestEvent;
import ch.admin.bit.jeap.messaging.transactionaloutbox.test.TestMessageKey;

interface TestEventSignatureListener {

    void receive(TestEvent testEvent, TestMessageKey messageKey,
                 String jeapSignature, String jeapSignatureKey, String jeapCertificate);

}
