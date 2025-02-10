package ch.admin.bit.jeap.messaging.kafka.test.integration;

import ch.admin.bit.jeap.crypto.api.KeyIdCryptoService;
import ch.admin.bit.jeap.crypto.api.KeyReferenceCryptoService;
import ch.admin.bit.jeap.messaging.api.MessageListener;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.kafka.test.KafkaIntegrationTestBase;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.CryptoServiceTestConfig;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeCreateDeclarationCommandBuilder;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeCreateDeclarationCommandConsumer;
import ch.admin.bit.jme.declaration.JmeCreateDeclarationCommand;
import lombok.extern.slf4j.Slf4j;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(
        classes = {JmeCreateDeclarationCommandConsumer.class, CryptoServiceTestConfig.class},
        properties = {"spring.application.name=jme-messaging-sender-service"})
@EnableAutoConfiguration
@DirtiesContext
@Slf4j
class SendReceiveEncryptedITBase extends KafkaIntegrationTestBase {

    protected static final String MESSAGE_PAYLOAD_TEXT = "some text";

    @Autowired
    protected KeyIdCryptoService keyIdCryptoService;

    @Autowired
    protected KeyReferenceCryptoService keyReferenceCryptoService;

    @MockitoBean
    protected MessageListener<JmeCreateDeclarationCommand> messageProcessor;

    @Captor
    protected ArgumentCaptor<JmeCreateDeclarationCommand> messageCaptor;


    protected void sendMessage() {
        AvroMessage message = JmeCreateDeclarationCommandBuilder.create()
                .idempotenceId("idempotenceId")
                .text(MESSAGE_PAYLOAD_TEXT)
                .build();
        sendSync(JmeCreateDeclarationCommandConsumer.TOPIC_NAME, message);
    }

}