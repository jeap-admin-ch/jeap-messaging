package ch.admin.bit.jeap.messaging.kafka.signature;

import lombok.experimental.UtilityClass;
import org.springframework.util.ResourceUtils;

import java.io.IOException;
import java.nio.file.Files;

/**
 * see src/test/resources/signing/unittest/README.txt for informations on the cert and key creation
 */
@UtilityClass
public final class SigningTestHelper {

    public static final String PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\n" + // trufflehog:ignore
            "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCowtD5p9xtalDd\n" +
            "eK10F0Wgyb9wXML3Qn8j7zmOcRIgBXuLABL8FO0a0DNgmadeDm1BVbA/3Jmb9Ci/\n" +
            "SbR561MS868Xwx4+5cr4d4fBfTH00ZSN2CwTdOU3YiBGvJvvhdPt5GE4/86u2poF\n" +
            "HsclixsncWHjNVrmoQZyc7lBq6Hau0I2au7/2Xr9vpraQRMA/2CSjZoNE7UgMJkR\n" +
            "d/4hgikXjK/9Wtzett/+tRXFWlPe+J01l5rlCVnkBztW9ILeEQVx+GVDkJ6aBEWX\n" +
            "sKmDHZyEN3F2ESa4Ttt56NlR0m5wLYnuwaS83lgi4pwgZC1jUx05Yf4/hD7ObXUV\n" +
            "ShNi3rBxAgMBAAECggEAFiiwcluLarV/RTlTgy8MFjLvZ2RSuE87rP78Hnszz08Q\n" +
            "+/0oQP/Ja1Ajst8yFfxMdbv33sbLR6o9UHKv1yNiXtZu6u6Ukdsv9cSSJ4KWFOiQ\n" +
            "+jgEf5CFiWphxQ/+ZKq8m1u1tVuHareq6hmyufgEOn9dhE0s8KP7mfsee+Q6piLh\n" +
            "2+oo9srZqH2kjRrUj8WoBzDmsunWVPRSxs370f/pTbxTmt7JmWSKQGE0myVZHU9w\n" +
            "tf2ckS6h4JvP1oKACRn+Q2qS9yFpCWp9JqmAkBJbCZ2/rnPFRMssJX7gnf9ZPbDO\n" +
            "QXtEBW1HevhKrQjqU+S8S3aQePFatw1TfzY0nLHojQKBgQDtUkXib30bN5msBs/k\n" +
            "8vcK+5Tcz+R7A1e0VKs8m7E3blx1RJuNzsLpaOFvIumZGVXayAWRhfYTLs7vdia5\n" +
            "VzvJMtud4mXrXx755qxP3YOVePY26UYUQdOCVShTEoZnRuiSJpnab7zXybJv6T1i\n" +
            "Y/EeHnX4IQW3K2oCV6hMrb0l9QKBgQC2CyNVrJdLRidmnnKBmF4fURPrIBavGbn0\n" +
            "bxJ0bY5ixU4WaCUTGOdDb/6QjruZzDOIonhPJ4rWN/wT+Dx5uolN58nKDu+gwOSj\n" +
            "oh93p35A2P/vX//sLldxT1KTPI26x1z2WGi3zIfZXZ7C5pSQgFYsSdVVpWt6aJeT\n" +
            "a1Es/vfXDQKBgCV9dr7Dn/7ZtLQBS3w+iZo5jhWn3c81Avjg20Ay1DcOfxqjYPNw\n" +
            "80eOIva8jCx8XRy3tnF7uRjkrxoTDyD+T9qD5z+00SbymuEdeKERPEUzm6mnBkQS\n" +
            "9gfDzh/5cDQGdp0H7gwc1Lc+DKszLFhLs35vj3FIPi/mctO150ddtuC1AoGATjtT\n" +
            "IApfqlWnsQ3+n3rfWA37xVuw4+bmZwkkoSxDuGaaPD7dT+uv1udWOsFcFagPrXRE\n" +
            "x59ypsTAa8SOGLH1N6mYqmDo0eiTWU7evwkX1L5snm5fCRyRL3yrO+MV9WKfrK4p\n" +
            "snMPHnaBN7jnt4GvhrDkIT5Jhf1UxmGkLPYVhDECgYAXCcL65CHZK0pW+KQKD7Ng\n" +
            "gxViGfakySLrbiOZtsZ0Uw8e46ktjA41FfQlO6Kc1IoNvcXE2mk4npZ4BaOBEpeG\n" +
            "5SyIo5c1riCvugIIWSXtlorximmdNQTPa0Hbxtc6GeDByUYcniWKsy4opcFFomKD\n" +
            "FanvQxuc/m9zml2fsja9ZQ==\n" +
            "-----END PRIVATE KEY-----\n"; // trufflehog:ignore

    public static byte[] readBytesFromFile(String path) {
        try {
            return Files.readAllBytes(
                    ResourceUtils.getFile(path).toPath()
            );
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}