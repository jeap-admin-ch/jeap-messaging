package ch.admin.bit.jeap.messaging.kafka.signature.common;

import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.CertificateException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.io.ByteArrayInputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.*;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

@Slf4j
@UtilityClass
public class CertificateHelper {

    private static final String CERTIFICATE_FACTORY_INSTANCE_NAME = "X.509";
    private static final String CERT_PATH_VALIDATOR_ALGORITHM = "PKIX";
    private static final String COMMON_NAME = "CN";

    public static String getCommonName(String distinguishedName) {
        try {
            LdapName ldapName = new LdapName(distinguishedName);
            for (Rdn rdn : ldapName.getRdns()) {
                if (rdn.getType().equalsIgnoreCase(COMMON_NAME)) {
                    return (String) rdn.getValue();
                }
            }
            return null;
        } catch (InvalidNameException e) {
            log.error("Could not parse CN from distinguishedName", e);
            throw CertificateException.parsingCertificateFailed(e);
        }
    }

    public static String serialNumberHexString(byte[] serialNumber){
        return HexFormat.ofDelimiter(" ").formatHex(serialNumber);
    }

    static X509Certificate generateCertificate(byte[] certificateBytes) {
        try {
            CertificateFactory certificateFactory = getCertificateFactory();
            return (X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(certificateBytes));
        } catch (java.security.cert.CertificateException exception) {
            throw CertificateException.creatingCertificateFailed(exception);
        }
    }

    /**
     * @param certificates a list of the certificates, last certificate in the list must be the root
     */
    public static void validateCertificateChain(List<X509Certificate> certificates) {
        try {
            CertPathValidator validator = CertPathValidator.getInstance(CERT_PATH_VALIDATOR_ALGORITHM);
            CertificateFactory certFactory = getCertificateFactory();

            // Build CertPath (Certificate chain)
            CertPath certPath = certFactory.generateCertPath(certificates);

            // Create Trust Anchor (Root CA) - last certificate in the list must be the root
            TrustAnchor trustAnchor = new TrustAnchor(certificates.getLast(), null);
            PKIXParameters params = new PKIXParameters(Set.of(trustAnchor));

            params.setRevocationEnabled(false); // Set to true if using a CRL or OCSP
            validator.validate(certPath, params);
        } catch (NoSuchAlgorithmException | java.security.cert.CertificateException |
                 InvalidAlgorithmParameterException | CertPathValidatorException exception) {
            throw CertificateException.validatingCertificateChainFailed(exception);
        }
    }

    private static CertificateFactory getCertificateFactory() throws java.security.cert.CertificateException {
        return CertificateFactory.getInstance(CERTIFICATE_FACTORY_INSTANCE_NAME);
    }
}
