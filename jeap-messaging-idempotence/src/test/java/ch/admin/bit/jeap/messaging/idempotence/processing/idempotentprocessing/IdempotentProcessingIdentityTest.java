package ch.admin.bit.jeap.messaging.idempotence.processing.idempotentprocessing;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class IdempotentProcessingIdentityTest {

    @Test
    void from_commandWithoutVersion_contextIsSetWithoutVersion(){
        IdempotentProcessingIdentity identity = IdempotentProcessingIdentity.from("id", "JmeCreateDeclarationCommand");
        assertThat(identity.getContext()).isEqualTo("JmeCreateDeclarationCommand");
    }

    @Test
    void from_commandWithVersion_contextIsSetWithoutVersion(){
        IdempotentProcessingIdentity identity = IdempotentProcessingIdentity.from("id", "JmeCreateDeclarationV2Command");
        assertThat(identity.getContext()).isEqualTo("JmeCreateDeclarationCommand");
    }

    @Test
    void from_eventWithoutVersion_contextIsSetWithoutVersion(){
        IdempotentProcessingIdentity identity = IdempotentProcessingIdentity.from("id", "JmeDeclarationCreatedEvent");
        assertThat(identity.getContext()).isEqualTo("JmeDeclarationCreatedEvent");
    }

    @Test
    void from_eventWithVersion_contextIsSetWithoutVersion(){
        IdempotentProcessingIdentity identity = IdempotentProcessingIdentity.from("id", "JmeDeclarationCreatedV2Event");
        assertThat(identity.getContext()).isEqualTo("JmeDeclarationCreatedEvent");
    }

    @Test
    void from_eventWithVersionWithMultipleDigits_contextIsSetWithoutVersion(){
        IdempotentProcessingIdentity identity = IdempotentProcessingIdentity.from("id", "JmeDeclarationCreatedV25234Event");
        assertThat(identity.getContext()).isEqualTo("JmeDeclarationCreatedEvent");
    }
}
