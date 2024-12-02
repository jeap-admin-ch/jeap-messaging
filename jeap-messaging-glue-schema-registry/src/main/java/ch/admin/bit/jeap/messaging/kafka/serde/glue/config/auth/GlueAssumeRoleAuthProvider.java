package ch.admin.bit.jeap.messaging.kafka.serde.glue.config.auth;

import ch.admin.bit.jeap.messaging.kafka.properties.cluster.GlueProperties;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.StsClientBuilder;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import java.time.Duration;

@Slf4j
public class GlueAssumeRoleAuthProvider implements GlueAuthProvider {

    private final AwsCredentialsProvider awsCredentialsProvider;
    private final GlueProperties properties;
    private final String sessionName;

    public GlueAssumeRoleAuthProvider(AwsCredentialsProvider awsCredentialsProvider,
                                      GlueProperties properties, String sessionName) {
        this.awsCredentialsProvider = awsCredentialsProvider;
        this.properties = properties;
        this.sessionName = sessionName;
    }

    @Override
    public AwsCredentialsProvider getAwsCredentialsProvider() {
        // Configure the AWS STS client for retrieving a token with the AssumeRole request. Allows for
        // overriding the STS endpoint to use a regional endpoint as recommended by AWS.
        StsClientBuilder stsClientBuilder = StsClient.builder()
                .credentialsProvider(awsCredentialsProvider)
                .region(Region.of(properties.getRegion()));
        if (properties.getStsEndpoint() != null) {
            stsClientBuilder.endpointOverride(properties.getStsEndpoint());
        }

        log.info("Building STS client with properties region={}, endpointOverride={}, timeout={}, assumeRolArn={}, sessionName={}",
                properties.getRegion(), properties.getStsEndpoint(), properties.getStsClientTimeoutSeconds(),
                properties.getAssumeIamRoleArn(), sessionName);

        // Configure the AWS STS AssumeRoleCredentialsProvider to retrieve a token with the AssumeRole request.
        // This makes sense for AWS Glue as the schema registry for a Kafka cluster will often be located in a separate
        // account from the applications.
        StsClient stsClient = stsClientBuilder
                .httpClientBuilder(UrlConnectionHttpClient.builder()
                        .connectionTimeout(Duration.ofSeconds(properties.getStsClientTimeoutSeconds()))
                        .socketTimeout(Duration.ofSeconds(properties.getStsClientTimeoutSeconds())))
                .build();
        return StsAssumeRoleCredentialsProvider.builder()
                .stsClient(stsClient)
                .refreshRequest(() -> {
                            log.debug("Refreshing STS credentials");
                            return AssumeRoleRequest.builder()
                                    .roleArn(properties.getAssumeIamRoleArn())
                                    .roleSessionName(sessionName)
                                    .build();
                        }
                )
                .build();
    }
}
