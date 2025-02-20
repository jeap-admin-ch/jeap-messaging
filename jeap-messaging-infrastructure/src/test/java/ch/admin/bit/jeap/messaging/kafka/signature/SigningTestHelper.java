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

    public static final String MESSAGE_RECEIVER_PRIVATE_KEY = "-----BEGIN PRIVATE KEY-----\n" + // trufflehog:ignore
            "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCveo5uK8Aw54pI\n" +
            "oC8h2HyBPq/8+0z5II7XxpEjLVs8RMXBl7A9wxUuan+PtWYk1hDzB3GQTc9ci48v\n" +
            "JTK858oX8Q41ava++yseYsjqg7zDc/9NV9b2O+ON4rHuOmgh1/OrTRA/zxSzzHKp\n" +
            "4FHK5cqE/pC3GjCr+XHcs7bcOI6jo4DUMwJjifyxlj/Cr92nKNmYRoyHcVPCChOC\n" +
            "GjWAXB+haxvug7/aCa0/vtOkLUxuz2WZ7aAY2U7eHkwdnmpaceba4GZIGqZ/eBqn\n" +
            "stl+aciWQWyoM/Isp+wzFDmsNBHSEpX2Yb0Oppd5ln7DCgeMTFsWM8dYWySAfAxy\n" +
            "dI3WWDnnAgMBAAECggEARqHqrc7fe+/fGg+cGSAGMZHVjdtrZsXjqVfseL/bfv1h\n" +
            "qmSP1LRvFLFaajKGjGI8DU7cN80SH/qu5jevXhlgn9VwNjE5Y7M+7mqt27OuKfBJ\n" +
            "fczTImcF18k9fJo26xR9KOEKntyQRTUQnG9GDQkExRagkoswtzBfVApkmu0LATN1\n" +
            "te2Z6VljM72K2OY9X04Xo7otpm+hT8yTzYFwBsruA6zDfG0nS7Y9SdIATzjdDSsH\n" +
            "IjsdxOoL9aUcgR7KUxXPDBUVAkZK4LxcagfPW7WeyTg872ERa0z/RVed1oh3Jlck\n" +
            "4TOTABIr5bx74dRG+Xn7RjnStjHUnw3BkbEivjIIoQKBgQC81MAIK/1Fd7XaYrm0\n" +
            "2gW4GVRHqlrXDD/A/1r7GrhFBJdSChEY2cHmd42OGUsd+2peWTBtE5rwucEaftY5\n" +
            "0G54+tbQjFgjlZFDtfcVSoGp44bQhtgZrTZBB9DhAm5iED2cHBBPo4fpfbwrwpST\n" +
            "6FKdbJNdXiXl9M9X4OL+NWspBwKBgQDt5exabE9m9DrTmqJYAim5wGorWq5K1aHC\n" +
            "5xRcTqQLiW9cKWRk7yMUMvkielwj4dr70YchcgD5MaTa9O5N+V3Pkksb3F0wpLnG\n" +
            "eg4p4avDRgiM1HwIW1vO/CsPHvh37JZOmBNYgjhBk17UEZSygPyDkIeiVgvRTFqa\n" +
            "okNh1uyQIQKBgFR6ug7t3zmc/PbfBEYs5DIg4tvNEybyi/NESufcZGQ8UZaaelur\n" +
            "0FvTULkqB7k6KbRcIpYqFz9rW0EHTcA/x6zITCKxJx2EZKDuX8ReuPPQnxfbvAKA\n" +
            "w60EWibQd6HXsjiNNZ7rgnqrjevl+aLSZWOYl7VF0Z63j6u5KNSiAXcxAoGBANSb\n" +
            "qVFefs/Z8UDvb6sLs9KoHbpFPoLuzbRr5Axi08TjvLw22dxCw6znqTOg+vuue5CV\n" +
            "vxiq1CfgsB5myxqwg6Bgc/OgS0CP1t+EcWgIoQLRcg66T2cAjto7Dxhia1hx/hqb\n" +
            "Wp69Mh2Y4STR2Xx2PjYuUqlIESOqA0czQNkNxTlBAoGATbNhjJkJoexFrD64O4wq\n" +
            "Y/cUwyecmdHrh7qP6rP2fGrDA0Nzqb4G3/9YY8J5YViMI/A4sItdg1YzJC+Gizut\n" +
            "TNcNKs07qZzhEea6F3oo+lUCteQd4wGWIzkuH5WtBcyZxYeul+BVjK9SeukYoozO\n" +
            "3ilinwK9j5qHDGPwOOgbmk0=\n" +
            "-----END PRIVATE KEY-----";

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
