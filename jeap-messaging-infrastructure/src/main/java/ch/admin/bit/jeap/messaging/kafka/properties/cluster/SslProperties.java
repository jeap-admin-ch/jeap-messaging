package ch.admin.bit.jeap.messaging.kafka.properties.cluster;

import lombok.Data;

@Data
public class SslProperties {
    /**
     * Location of the key store for the mutual authentication.
     */
    String keyStoreLocation;
    /**
     * Type of the key store. valid options: [JKS, PKCS12, PEM]
     */
    String keyStoreType;
    /**
     * Password for the key.
     */
    String keyPassword;
    /**
     * Location of the trust store.
     */
    String trustStoreLocation;
    /**
     * Type of the trust store. valid options: [JKS, PKCS12, PEM]
     */
    String trustStoreType;
    /**
     * Password for the trust store.
     */
    String trustStorePassword;

}
