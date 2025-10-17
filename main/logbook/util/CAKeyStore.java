package logbook.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import logbook.constants.AppConstants;

public class CAKeyStore {
    public static void genrateIfNeeded() throws NoSuchAlgorithmException, OperatorCreationException,
            CertificateException, KeyStoreException, FileNotFoundException, IOException {
        if (AppConstants.PKCS12_FILE.exists()) {
            System.out.println("PKCS12ファイルが存在しているため作成スキップ");
            return;
        }

        Security.addProvider(new BouncyCastleProvider());

        // 1.鍵ペア生成
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        // 2. 自己署名 X.509 証明書作成（100年有効）
        X500Name owner = new X500Name("CN=LogbookCA, OU=MITM, O=MyOrg, L=Tokyo, ST=Tokyo, C=JP");
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date();
        Date notAfter = new Date(notBefore.getTime() + 36500L * 24 * 60 * 60 * 1000); // 100年

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                owner,
                serial,
                notBefore,
                notAfter,
                owner,
                keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                .setProvider("BC")
                .build(keyPair.getPrivate());

        X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certBuilder.build(signer));

        // 3. PKCS12 キーストアに格納
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        ks.setKeyEntry("logbook", keyPair.getPrivate(), AppConstants.PKCS12_PASSWORD.toCharArray(),
                new X509Certificate[] { cert });

        // 4. ファイル出力
        try (FileOutputStream fos = new FileOutputStream(AppConstants.PKCS12_FILE)) {
            ks.store(fos, AppConstants.PKCS12_PASSWORD.toCharArray());
        }

        // 5. PEM 形式で秘密鍵・証明書も出力
        // 秘密鍵 PEM
        try (Writer out = new FileWriter(AppConstants.KEY_FILE)) {
            out.write("-----BEGIN PRIVATE KEY-----\n");
            out.write(Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(keyPair.getPrivate().getEncoded()));
            out.write("\n-----END PRIVATE KEY-----\n");
        }

        // 証明書 PEM
        try (Writer out = new FileWriter(AppConstants.CRT_FILE)) {
            out.write("-----BEGIN CERTIFICATE-----\n");
            out.write(Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(cert.getEncoded()));
            out.write("\n-----END CERTIFICATE-----\n");
        }
    }
}
