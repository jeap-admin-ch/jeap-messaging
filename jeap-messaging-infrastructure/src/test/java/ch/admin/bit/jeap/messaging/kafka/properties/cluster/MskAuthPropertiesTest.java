package ch.admin.bit.jeap.messaging.kafka.properties.cluster;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MskAuthPropertiesTest {

    @Test
    void validateProperties() {
        MskAuthProperties props = new MskAuthProperties();
        props.setIamAuthEnabled(true);

        assertThatCode(props::validateProperties)
                .doesNotThrowAnyException();
    }

    @Test
    void validateProperties_shouldThrow_whenNoRegionRoleIsGivenForAssumeRoleAuth() {
        MskAuthProperties props = new MskAuthProperties();
        props.setIamAuthEnabled(true);
        props.setAssumeIamRoleArn("arn");

        assertThatThrownBy(props::validateProperties)
                .hasMessageContaining("region");
    }

    @Test
    void validateProperties_shouldNotThrow_whenValidAssumeRolePropertiesAreSet() {
        MskAuthProperties props = new MskAuthProperties();
        props.setIamAuthEnabled(true);
        props.setAssumeIamRoleArn("arn");
        props.setRegion("eu-test-1");

        assertThatCode(props::validateProperties)
                .doesNotThrowAnyException();
    }
}
