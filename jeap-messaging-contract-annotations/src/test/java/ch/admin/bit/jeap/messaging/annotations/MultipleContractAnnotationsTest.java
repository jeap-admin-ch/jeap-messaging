package ch.admin.bit.jeap.messaging.annotations;

import org.junit.jupiter.api.Test;

import static ch.admin.bit.jeap.messaging.annotations.ContractAnnotationsTestHelper.assertContractFileExists;

class MultipleContractAnnotationsTest {

    public static final String APP_NAME = "appNameForTest";

    @Test
    @JeapMessageConsumerContract(value = TypeRefExamples.TypeRef2.class, appName = APP_NAME, topic = {"topic1", "topic2"})
    @JeapMessageConsumerContract(value = TypeRefExamples.TypeRef3.class, appName = APP_NAME, topic = {"topic1", "topic2"})
    void generatedConsumerContractsExist() {
        String expectedName_1 = APP_NAME + "-" + TypeRefExamples.TypeRef2.MESSAGE_TYPE_NAME + "-" + TypeRefExamples.TypeRef2.MESSAGE_TYPE_VERSION + ".consumer-contract.json";
        assertContractFileExists(expectedName_1);

        String expectedName_2 = APP_NAME + "-" + TypeRefExamples.TypeRef3.MESSAGE_TYPE_NAME + "-" + TypeRefExamples.TypeRef3.MESSAGE_TYPE_VERSION + ".consumer-contract.json";
        assertContractFileExists(expectedName_2);
    }

    @Test
    @JeapMessageProducerContract(value = TypeRefExamples.TypeRef2.class, appName = APP_NAME, topic = {"topic1", "topic2"})
    @JeapMessageProducerContract(value = TypeRefExamples.TypeRef3.class, appName = APP_NAME, topic = {"topic1", "topic2"})
    void generatedProducersContractsExist() {
        String expectedName_1 = APP_NAME + "-" + TypeRefExamples.TypeRef2.MESSAGE_TYPE_NAME + "-" + TypeRefExamples.TypeRef2.MESSAGE_TYPE_VERSION + ".producer-contract.json";
        assertContractFileExists(expectedName_1);

        String expectedName_2 = APP_NAME + "-" + TypeRefExamples.TypeRef3.MESSAGE_TYPE_NAME + "-" + TypeRefExamples.TypeRef3.MESSAGE_TYPE_VERSION + ".producer-contract.json";
        assertContractFileExists(expectedName_2);
    }

    @Test
    @JeapMessageProducerContracts(value = {TypeRefExamples.TypeRef7.class, TypeRefExamples.TypeRef8.class}, appName = APP_NAME)
    void generatedMultiProducerContractsExist() {
        String expectedName_1 = APP_NAME + "-" + TypeRefExamples.TypeRef7.MESSAGE_TYPE_NAME + "-" + TypeRefExamples.TypeRef7.MESSAGE_TYPE_VERSION + ".producer-contract.json";
        assertContractFileExists(expectedName_1);

        String expectedName_2 = APP_NAME + "-" + TypeRefExamples.TypeRef8.MESSAGE_TYPE_NAME + "-" + TypeRefExamples.TypeRef8.MESSAGE_TYPE_VERSION + ".producer-contract.json";
        assertContractFileExists(expectedName_2);
    }

    @Test
    @JeapMessageConsumerContracts(value = {TypeRefExamples.TypeRef7.class, TypeRefExamples.TypeRef8.class}, appName = APP_NAME)
    void generatedMultiConsumerContractsExist() {
        String expectedName_1 = APP_NAME + "-" + TypeRefExamples.TypeRef7.MESSAGE_TYPE_NAME + "-" + TypeRefExamples.TypeRef7.MESSAGE_TYPE_VERSION + ".consumer-contract.json";
        assertContractFileExists(expectedName_1);

        String expectedName_2 = APP_NAME + "-" + TypeRefExamples.TypeRef8.MESSAGE_TYPE_NAME + "-" + TypeRefExamples.TypeRef8.MESSAGE_TYPE_VERSION + ".consumer-contract.json";
        assertContractFileExists(expectedName_2);
    }
}