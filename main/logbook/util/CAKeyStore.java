package logbook.util;

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
            CertificateException, KeyStoreException, IOException {
        if (AppConstants.PKCS12_FILE.exists()) {
            System.out.println("PKCS12ファイルが存在しているため作成スキップします。");
            return;
        }

        Security.addProvider(new BouncyCastleProvider());

        // 1.鍵ペア生成
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();

        // 2. 自己署名 X.509 証明書作成（100年有効）
        X500Name owner = new X500Name("CN=" + AppConstants.CN_ALIAS + ", OU=MITM, O=MyOrg, L=Tokyo, ST=Tokyo, C=JP");
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

    public static void installCertificateIfNeeded() throws InterruptedException, IOException, NoSuchAlgorithmException,
            CertificateException, KeyStoreException {
        String os = System.getProperty("os.name").toLowerCase();
        if (!AppConstants.CRT_FILE.exists()) {
            System.out.println("CRTファイルが存在していないためインストールスキップします。");
            return;
        }

        if (os.contains("win")) {
            installToWindows();
        }
        else if (os.contains("linux")) {
            installToLinux();
        }
        else if (os.contains("mac")) {
            installToMac();
        }
    }

    private static boolean existsTrustedRootCertificationAuthorities() throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe",
                "-Command",
                "if (Get-ChildItem -Path Cert:\\CurrentUser\\Root | Where-Object { $_.Subject -like '*"
                        + AppConstants.CN_ALIAS + "*' }) { exit 0 } else { exit 1 }");
        return pb.start().waitFor() == 0;
    }

    /**
     * Windows のユーザー信頼ストアにインポート
     * @throws InterruptedException 
     * @throws IOException 
     */
    private static int installToWindows() throws InterruptedException, IOException {
        if (existsTrustedRootCertificationAuthorities()) {
            System.out.println("証明書は Windows の信頼されたルート証明機関に登録されているためスキップします。");
            return 0;
        }
        else {
            System.out.println("証明書はまだ登録されていません。");
        }

        ProcessBuilder pb = new ProcessBuilder(
                "powershell.exe",
                "-Command",
                "Import-Certificate -FilePath \"" + AppConstants.CRT_FILE
                        + "\" -CertStoreLocation \"Cert:\\CurrentUser\\Root\"");
        return pb.inheritIO().start().waitFor();
    }

    private static boolean existsSystemTrustedRootCertificate() throws IOException, InterruptedException {
        String os = System.getProperty("os.name").toLowerCase();
        String command;

        if (os.contains("mac")) {
            command = "security find-certificate -a -c \"" + AppConstants.CN_ALIAS
                    + "\" /Library/Keychains/System.keychain >/dev/null 2>&1";
        }
        else {
            command = "openssl verify -CApath /etc/ssl/certs <(openssl x509 -in " + AppConstants.CRT_FILE.getName()
                    + ") >/dev/null 2>&1";
        }

        ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
        return pb.start().waitFor() == 0;
    }

    /**
     * Linux のシステムCAストアにインポート
     * @throws IOException 
     * @throws InterruptedException 
     */
    private static int installToLinux() throws IOException, InterruptedException {
        if (existsSystemTrustedRootCertificate()) {
            System.out.println("証明書は Linux のシステムCAに登録されているためスキップします。");
            return 0;
        }
        else {
            System.out.println("証明書はまだ登録されていません。");
        }

        ProcessBuilder checkPb = new ProcessBuilder("bash", "-c", "command -v pkexec");
        if (checkPb.start().waitFor() != 0) {
            // pkexec が存在しない場合はスキップ
            System.out.println("pkexec が見つかりません。システムCAへの登録をスキップします。");
            return 0;
        }

        String command = "pkexec bash -c 'cp " + AppConstants.CRT_FILE
                + " /usr/local/share/ca-certificates/ && update-ca-certificates'";
        return new ProcessBuilder("bash", "-c", command).inheritIO().start().waitFor();
    }

    /**
     * Mac のシステムCAストアにインポート
     * @return
     * @throws InterruptedException
     * @throws IOException
     */
    private static int installToMac() throws InterruptedException, IOException {
        if (existsSystemTrustedRootCertificate()) {
            System.out.println("証明書は Mac のシステムCAに登録されているためスキップします。");
            return 0;
        }
        else {
            System.out.println("証明書はまだ登録されていません。");
        }

        String command = String.format(
                "osascript -e 'do shell script \"security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain %s\" with administrator privileges'",
                AppConstants.CRT_FILE);
        return new ProcessBuilder("bash", "-c", command).inheritIO().start().waitFor();
    }
}
