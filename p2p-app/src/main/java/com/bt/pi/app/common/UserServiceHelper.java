package com.bt.pi.app.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.annotation.Resource;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.PiCertificate;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.util.DigestUtils;
import com.bt.pi.app.common.util.HashDigest;
import com.bt.pi.app.common.util.SecurityUtils;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.dht.DhtClientFactory;

@Component
public class UserServiceHelper {
    private static final Log LOG = LogFactory.getLog(UserServiceHelper.class);
    private static final String S_S = "%s%s";
    private static final String CERT_PEM = "-cert.pem";
    private static final String PK_PEM = "-pk.pem";
    private static final String USER_CERT_DN = "CN=www.cloud21cn.com, OU=Pi, O=%s, L=London, ST=England, C=UK";
    private static final int EIGHT = 8;
    private static final int ACCESS_KEY_LENGTH = 20;

    private static final String UNIX_CURRENT_DIR_COMMAND = "$(dirname $(readlink -f ${BASH_SOURCE}))";
    private static final String UNIX_SET_COMMAND = "export";
    private static final String UNIX_ALIAS_COMMAND = "alias";
    private static final String UNIX_PATH_SEPARATOR = "/";
    private static final String UNIX_START_OF_MACRO = "${";
    private static final String UNIX_END_OF_MACRO = "}";
    private static final String UNIX_LINE_SEPARATOR = "\n";

    private static final String WINDOWS_CURRENT_DIR_COMMAND = "%CD%";
    private static final String WINDOWS_SET_COMMAND = "set";
    private static final String WINDOWS_ALIAS_COMMAND = "doskey";
    private static final String WINDOWS_PATH_SEPARATOR = "\\";
    private static final String WINDOWS_START_OF_MACRO = "%";
    private static final String WINDOWS_END_OF_MACRO = WINDOWS_START_OF_MACRO;
    private static final String WINDOWS_LINE_SEPARATOR = "\r\n";

    private Commands unixCommands;
    private Commands windowsCommands;

    private String certDn;
    private String keyAlgorithm;
    private String keySigningAlgorithm;
    private int keySize;

    private String piUrl;
    private String pisssUrl;
    private PiCertificate piCertificate;

    private SecurityUtils securityUtils;
    private DhtClientFactory dhtClientFactory;
    private PiIdBuilder piIdBuilder;
    private DigestUtils digestUtils;

    public UserServiceHelper() {
        unixCommands = new Commands(UNIX_SET_COMMAND, UNIX_CURRENT_DIR_COMMAND, UNIX_ALIAS_COMMAND, UNIX_PATH_SEPARATOR, UNIX_START_OF_MACRO, UNIX_END_OF_MACRO, UNIX_LINE_SEPARATOR);
        windowsCommands = new Commands(WINDOWS_SET_COMMAND, WINDOWS_CURRENT_DIR_COMMAND, WINDOWS_ALIAS_COMMAND, WINDOWS_PATH_SEPARATOR, WINDOWS_START_OF_MACRO, WINDOWS_END_OF_MACRO, WINDOWS_LINE_SEPARATOR);

        piCertificate = null;
        securityUtils = null;
        dhtClientFactory = null;
        piIdBuilder = null;
        digestUtils = null;
    }

    @Resource
    public void setSecurityUtils(SecurityUtils aSecurityUtils) {
        securityUtils = aSecurityUtils;
    }

    @Resource
    public void setDigestUtils(DigestUtils aDigestUtils) {
        this.digestUtils = aDigestUtils;
    }

    @Resource
    public synchronized void setDhtClientFactory(DhtClientFactory adhtClientFactory) {
        dhtClientFactory = adhtClientFactory;
    }

    @Resource
    public synchronized void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        piIdBuilder = aPiIdBuilder;
    }

    @Property(key = "security.cert.dn", defaultValue = "CN=www.cloud21cn.com, OU=Pi, O=BT, L=London, ST=England, C=UK")
    public void setCertDn(String value) {
        certDn = value;
    }

    @Property(key = "security.key.generator.algorithm", defaultValue = "RSA")
    public void setKeyAlgorithm(String value) {
        keyAlgorithm = value;
    }

    @Property(key = "security.key.generator.signing.algorithm", defaultValue = "SHA512WithRSA")
    public void setKeySigningAlgorithm(String value) {
        keySigningAlgorithm = value;
    }

    @Property(key = "security.key.generator.size", defaultValue = "2048")
    public void setKeySize(int value) {
        keySize = value;
    }

    @Property(key = "pi.url", defaultValue = "pi url")
    public void setPiUrl(String value) {
        piUrl = value;
    }

    @Property(key = "pisss.url", defaultValue = "pi-sss url")
    public void setPisssUrl(String value) {
        pisssUrl = value;
    }

    public synchronized PiCertificate getPiCertificate() {
        LOG.debug("getPiCertificate()");
        if (piCertificate == null) {
            piCertificate = (PiCertificate) dhtClientFactory.createBlockingReader().get(piIdBuilder.getPId(new PiCertificate()));
        }
        LOG.debug(String.format("returning: %s", piCertificate));
        return piCertificate;
    }

    public PiCertificate createPiCertificate() throws GeneralSecurityException {
        LOG.debug("createPiCertificate()");
        KeyPair keyPair = securityUtils.getNewKeyPair(keyAlgorithm, keySize);
        X509Certificate certificate = securityUtils.getNewCertificate(certDn, keySigningAlgorithm, keyPair);
        certificate.checkValidity();

        return new PiCertificate(certificate.getEncoded(), keyPair.getPrivate().getEncoded(), keyPair.getPublic().getEncoded());
    }

    public String generateSecretKey(String username) {
        LOG.debug(String.format("generateSecretKey(%s)", username));
        return digestUtils.getDigestBase64(username, HashDigest.SHA224, true);
    }

    public String generateAccessKey(String username) {
        LOG.debug(String.format("generateAccessKey(%s)", username));
        return getStringOfSize(digestUtils.getDigestBase62(username, HashDigest.MD5, false), ACCESS_KEY_LENGTH);
    }

    private String getStringOfSize(String string, int length) {
        LOG.debug(String.format("getStringOfSize(%s, %d)", string, length));
        int diff = string.length() - length;
        if (diff == 0)
            return string;

        if (diff < 0) {
            StringBuilder stringBuilder = new StringBuilder(string);
            for (int i = string.length(); i < length; i++)
                stringBuilder.insert(0, '0');
            return stringBuilder.toString();
        }

        return string.substring(0, length);
    }

    public byte[] setUserCertAndKeysAndGetX509Zip(User user) throws GeneralSecurityException, IOException {
        LOG.debug(String.format("setUserCertAndKeysAndGetX509Zip(%s)", user));
        String username = user.getUsername();
        user.setApiSecretKey(generateSecretKey(username));
        user.setApiAccessKey(generateAccessKey(username));

        return setUserCertAndGetX509Zip(user);
    }

    public byte[] setUserCertAndGetX509Zip(User user) throws GeneralSecurityException, IOException {
        LOG.debug(String.format("setUserCertAndGetX509Zip(%s)", user));
        String username = user.getUsername();
        String userSecretKey = user.getApiSecretKey();
        String userAccessKey = user.getApiAccessKey();

        KeyPair keyPair = securityUtils.getNewKeyPair(keyAlgorithm, keySize);
        X509Certificate userCertificate = securityUtils.getNewCertificate(String.format(USER_CERT_DN, username), keySigningAlgorithm, keyPair);
        userCertificate.checkValidity();
        user.setCertificate(userCertificate.getEncoded());

        Certificate cloudCert = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(getPiCertificate().getCertificate()));

        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        ZipOutputStream zipOut = new ZipOutputStream(byteOut);
        zipOut.setComment("To setup the environment run: source /path/to/pirc");

        String baseName = String.format("pi-%s-%s", username, securityUtils.getFingerPrint(keyPair.getPublic()).replaceAll(":", "").toLowerCase(Locale.ENGLISH).substring(0, EIGHT));

        zipOut.putNextEntry(new ZipEntry("pirc"));
        zipOut.write(getPircFileContentsForUnix(baseName, username, userSecretKey, userAccessKey).getBytes());
        zipOut.closeEntry();

        zipOut.putNextEntry(new ZipEntry("pirc.bat"));
        zipOut.write(getPircFileContentsForWindows(baseName, username, userSecretKey, userAccessKey).getBytes());
        zipOut.closeEntry();

        zipOut.putNextEntry(new ZipEntry("cloud-cert.pem"));
        zipOut.write(securityUtils.getPemBytes(cloudCert));
        zipOut.closeEntry();

        zipOut.putNextEntry(new ZipEntry(String.format(S_S, baseName, PK_PEM)));
        zipOut.write(securityUtils.getPemBytes(keyPair.getPrivate()));
        zipOut.closeEntry();

        zipOut.putNextEntry(new ZipEntry(String.format(S_S, baseName, CERT_PEM)));
        zipOut.write(securityUtils.getPemBytes(userCertificate));
        zipOut.closeEntry();

        zipOut.close();

        return byteOut.toByteArray();
    }

    private String getPircFileContentsForUnix(String baseName, String username, String userSecretKey, String userAccessKey) {
        return getPircFileContents(unixCommands, baseName, username, userSecretKey, userAccessKey);
    }

    private String getPircFileContentsForWindows(String baseName, String username, String userSecretKey, String userAccessKey) {
        return getPircFileContents(windowsCommands, baseName, username, userSecretKey, userAccessKey);
    }

    private String getPircFileContents(Commands commands, String baseName, String username, String userSecretKey, String userAccessKey) {
        StringBuffer sb = new StringBuffer();
        sb.append(String.format("%s PI_KEY_DIR=%s", commands.getSetCommand(), commands.getCurrentDirectoryCommand()));
        sb.append(String.format("%s%s S3_URL=%s", commands.getLineSeparator(), commands.getSetCommand(), pisssUrl));
        sb.append(String.format("%s%s EC2_URL=%s", commands.getLineSeparator(), commands.getSetCommand(), piUrl));
        sb.append(String.format("%s%s EC2_PRIVATE_KEY=%sPI_KEY_DIR%s%s%s%s", commands.getLineSeparator(), commands.getSetCommand(), commands.getStartOfMacro(), commands.getEndOfMacro(), commands.getPathSeparator(), baseName, PK_PEM));
        sb.append(String.format("%s%s EC2_CERT=%sPI_KEY_DIR%s%s%s%s", commands.getLineSeparator(), commands.getSetCommand(), commands.getStartOfMacro(), commands.getEndOfMacro(), commands.getPathSeparator(), baseName, CERT_PEM));
        sb.append(String.format("%s%s PI_CERT=%sPI_KEY_DIR%s%scloud-cert.pem", commands.getLineSeparator(), commands.getSetCommand(), commands.getStartOfMacro(), commands.getEndOfMacro(), commands.getPathSeparator()));
        sb.append(String.format("%s%s EC2_ACCESS_KEY='%s'", commands.getLineSeparator(), commands.getSetCommand(), userAccessKey));
        sb.append(String.format("%s%s EC2_SECRET_KEY='%s'", commands.getLineSeparator(), commands.getSetCommand(), userSecretKey));
        sb.append(String.format("%s%s EC2_USER_ID='%s'", commands.getLineSeparator(), commands.getSetCommand(), username));
        sb.append(String.format("%s%s ec2-bundle-image=\"ec2-bundle-image --cert %sEC2_CERT%s --privatekey %sEC2_PRIVATE_KEY%s --user 000000000000 --ec2cert %sPI_CERT%s\"", commands.getLineSeparator(), commands.getAliasCommand(),
                commands.getStartOfMacro(), commands.getEndOfMacro(), commands.getStartOfMacro(), commands.getEndOfMacro(), commands.getStartOfMacro(), commands.getEndOfMacro()));
        sb.append(String.format("%s%s ec2-upload-bundle=\"ec2-upload-bundle -a %sEC2_ACCESS_KEY%s -s %sEC2_SECRET_KEY%s --url %sS3_URL%s --ec2cert %sPI_CERT%s\"", commands.getLineSeparator(), commands.getAliasCommand(), commands.getStartOfMacro(),
                commands.getEndOfMacro(), commands.getStartOfMacro(), commands.getEndOfMacro(), commands.getStartOfMacro(), commands.getEndOfMacro(), commands.getStartOfMacro(), commands.getEndOfMacro()));
        sb.append(String.format("%s%s ec2-delete-bundle=\"ec2-delete-bundle -a %sEC2_ACCESS_KEY%s -s %sEC2_SECRET_KEY%s --url %sS3_URL%s\"", commands.getLineSeparator(), commands.getAliasCommand(), commands.getStartOfMacro(),
                commands.getEndOfMacro(), commands.getStartOfMacro(), commands.getEndOfMacro(), commands.getStartOfMacro(), commands.getEndOfMacro()));
        sb.append(commands.getLineSeparator());
        return sb.toString();
    }

    public byte[] decodeBase64(String encodedCertificate) {
        return Base64.decodeBase64(encodedCertificate);
    }

    private class Commands {
        private String setCommand;
        private String currentDirectoryCommand;
        private String aliasCommand;
        private String pathSeparator;
        private String startOfMacro;
        private String endOfMacro;
        private String lineSeparator;

        public Commands(String aSetCommand, String aKeyDirectoryCommand, String anAliasCommand, String aPathSeparator, String aStartOfMacro, String aEndOfMacro, String aLineSeparator) {
            setCommand = aSetCommand;
            currentDirectoryCommand = aKeyDirectoryCommand;
            aliasCommand = anAliasCommand;
            pathSeparator = aPathSeparator;
            startOfMacro = aStartOfMacro;
            endOfMacro = aEndOfMacro;
            lineSeparator = aLineSeparator;
        }

        public String getSetCommand() {
            return setCommand;
        }

        public String getCurrentDirectoryCommand() {
            return currentDirectoryCommand;
        }

        public String getAliasCommand() {
            return aliasCommand;
        }

        public String getPathSeparator() {
            return pathSeparator;
        }

        public String getStartOfMacro() {
            return startOfMacro;
        }

        public String getEndOfMacro() {
            return endOfMacro;
        }

        public String getLineSeparator() {
            return lineSeparator;
        }
    }

}
