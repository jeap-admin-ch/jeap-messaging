package ch.admin.bit.jeap.messaging.kafka.signature;

import ch.admin.bit.jeap.messaging.kafka.signature.exceptions.CertificateException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

@Slf4j
@UtilityClass
public class CertificateHelper {

    private static final String COMMON_NAME = "CN";

    static String getCommonName(String distinguishedName) {
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

}