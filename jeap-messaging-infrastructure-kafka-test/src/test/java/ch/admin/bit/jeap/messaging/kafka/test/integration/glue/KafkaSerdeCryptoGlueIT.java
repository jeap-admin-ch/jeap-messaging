package ch.admin.bit.jeap.messaging.kafka.test.integration.glue;

import ch.admin.bit.jeap.crypto.api.KeyId;
import ch.admin.bit.jeap.crypto.api.KeyIdCryptoService;
import ch.admin.bit.jeap.crypto.api.KeyReferenceCryptoService;
import ch.admin.bit.jeap.messaging.avro.AvroMessage;
import ch.admin.bit.jeap.messaging.avro.AvroMessageKey;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.CryptoServiceTestConfig;
import ch.admin.bit.jeap.messaging.kafka.test.integration.common.JmeCreateDeclarationCommandBuilder;
import ch.admin.bit.jme.declaration.JmeCreateDeclarationCommand;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.eq;

@SpringBootTest(classes = {TestGlueConfig.class}, properties = {
        "spring.application.name=jme-messaging-receiverpublisher-service",
        "spring.kafka.template.default-topic=default-test-topic",
        "jeap.messaging.kafka.embedded=false",
        "jeap.messaging.kafka.systemName=test",
        "jeap.messaging.kafka.errorTopicName=errorTopic",
        "jeap.messaging.kafka.message-type-encryption-disabled=true",
        "jeap.messaging.kafka.cluster.aws.aws.glue.registryName=testregistry",
        "jeap.messaging.kafka.cluster.aws.aws.glue.region=eu-test-1",
        "jeap.messaging.kafka.cluster.aws.bootstrapServers=${spring.embedded.kafka.brokers}",
        "jeap.messaging.kafka.cluster.aws.securityProtocol=PLAINTEXT",
        "jeap.messaging.kafka.exposeMessageKeyToConsumer=true",
        "jeap.messaging.kafka.message-type-encryption-disabled=false"
})
@ActiveProfiles("key-id-crypto-service")
@Import({KafkaSerdeCryptoGlueIT.TestConsumerConfig.class, CryptoServiceTestConfig.class})
class KafkaSerdeCryptoGlueIT extends KafkaGlueIntegrationTestBase {

    @Autowired
    protected KeyIdCryptoService keyIdCryptoService;

    @Autowired
    protected KeyReferenceCryptoService keyReferenceCryptoService;

    @Qualifier("aws")
    @Autowired
    protected KafkaTemplate<AvroMessageKey, AvroMessage> awsKafkaTemplate;

    @Qualifier("aws")
    @Autowired
    protected KafkaAdmin awsKafkaAdmin;

    @Captor
    ArgumentCaptor<byte[]> plainMessageCaptor;

    @Captor
    ArgumentCaptor<byte[]> encryptedMessageCaptor;

    static final String CREATE_DECLARATION_COMMAND_AVRO_SCHEMA = JmeCreateDeclarationCommand.SCHEMA$.toString().replace("\"", "\\\"");

    @Test
    void testSendAndReceiveEncrypted() {
        final KeyId testKeyId = KeyId.of("testKey");

        UUID createDeclarationCommandVersionId = UUID.randomUUID();
        stubGetSchemaByDefinitionResponse(createDeclarationCommandVersionId, "jme-messaging-create-declaration-JmeCreateDeclarationCommand");
        stubGetSchemaVersionResponse(createDeclarationCommandVersionId, CREATE_DECLARATION_COMMAND_AVRO_SCHEMA);
        JmeCreateDeclarationCommand createDeclarationCommand = JmeCreateDeclarationCommandBuilder.create()
                .idempotenceId(UUID.randomUUID().toString())
                .text("text")
                .build();
        sendSync(awsKafkaTemplate, JmeCreateDeclarationCommand.TypeRef.DEFAULT_TOPIC, createDeclarationCommand);

        await().until(() -> testConsumer.getCreateDeclarationCommands().size() == 1);

        Mockito.verify(keyIdCryptoService).encrypt(plainMessageCaptor.capture(), eq(testKeyId) );
        Mockito.verify(keyIdCryptoService).decrypt(encryptedMessageCaptor.capture());
        byte[] plainMessage = plainMessageCaptor.getValue();
        byte[] encryptedMessage = encryptedMessageCaptor.getValue();
        assertThat(plainMessage).isNotEqualTo(encryptedMessage);
        assertThat(encryptedMessage).isEqualTo(keyIdCryptoService.encrypt(plainMessage, testKeyId));
    }
}
