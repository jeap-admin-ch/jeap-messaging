package ch.admin.bit.jeap.messaging.contract.v2;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Shared naming rules for schema resources accompanying contracts. */
public final class SchemaResources {
    private SchemaResources() {
    }

    public static String fileName(String binaryClassName, Contract contract) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            for (String value : new String[]{binaryClassName, contract.getSystemName(), contract.getMessageTypeName(),
                    contract.getMessageTypeVersion(), contract.getRegistryUrl(), contract.getRegistryBranch(), contract.getRegistryCommit()}) {
                String part = value == null ? "-1:" : value.length() + ":" + value;
                digest.update(part.getBytes(StandardCharsets.UTF_8));
            }
            return "schema-" + HexFormat.of().formatHex(digest.digest()) + ".avsc";
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    public static String legacyFileName(Contract contract) {
        return contract.getMessageTypeName() + "-" + contract.getMessageTypeVersion() + ".avsc";
    }
}
