package ch.admin.bit.jeap.messaging.kafka.serde.glue.config.auth;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;

@FunctionalInterface
public interface GlueAuthProvider {

    AwsCredentialsProvider getAwsCredentialsProvider();
}
