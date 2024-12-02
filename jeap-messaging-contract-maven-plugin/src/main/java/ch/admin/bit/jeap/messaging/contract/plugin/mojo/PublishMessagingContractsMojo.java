package ch.admin.bit.jeap.messaging.contract.plugin.mojo;

import ch.admin.bit.jeap.messaging.contract.plugin.publish.MessageContractPublisher;
import ch.admin.bit.jeap.messaging.contract.plugin.publish.MessageContractServiceClient;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;

@Mojo(
        name = "publish-messaging-contracts",
        requiresDependencyResolution = ResolutionScope.RUNTIME,
        defaultPhase = LifecyclePhase.NONE,
        threadSafe = true)
public class PublishMessagingContractsMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(property = "messageContractServiceBaseUri", required = true, readonly = true)
    String messageContractServiceBaseUri;

    @Parameter(defaultValue = "${project.build.outputDirectory}/ch/admin/bit/jeap/messaging/contracts", required = true, readonly = true)
    File contractPath;

    @Parameter(property = "messageContractTransactionId", readonly = true)
    String transactionId;

    @Override
    public void execute() throws MojoFailureException {
        String username = getProperty("MESSAGE_CONTRACT_SERVICE_USERNAME");
        String password = getProperty("MESSAGE_CONTRACT_SERVICE_PASSWORD");
        if (username == null || password == null) {
            throw new MojoFailureException("Missing message contract service credentials " +
                    "(expected env vars or properties MESSAGE_CONTRACT_SERVICE_USERNAME and MESSAGE_CONTRACT_SERVICE_PASSWORD to be available)");
        }

        MessageContractServiceClient messageContractServiceClient =
                new MessageContractServiceClient(messageContractServiceBaseUri, getLog(), username, password);
        MessageContractPublisher messageContractPublisher = new MessageContractPublisher(messageContractServiceClient, getLog());
        String appVersion = project.getVersion();
        messageContractPublisher.publishMessageContracts(contractPath.toPath(), appVersion, transactionId);
    }

    /**
     * Credentials should come from env vars on the build server. This method is mostly meant to be able to
     * supply credentials via maven properties for tests.
     */
    private String getProperty(String propertyName) {
        String value = project.getProperties().getProperty(propertyName);
        if (value != null) {
            return value;
        }
        return System.getenv(propertyName);
    }
}
