package com.bt.pi.app.common.entities;

import com.bt.pi.core.entity.PiEntityBase;

public class PiCertificate extends PiEntityBase {
    private byte[] certificate;
    private byte[] privateKey;
    private byte[] publicKey;

    public PiCertificate() {
    }

    public PiCertificate(byte[] aCertificate, byte[] aPrivateKey, byte[] aPublicKey) {
        setCertificate(aCertificate);
        setPrivateKey(aPrivateKey);
        setPublicKey(aPublicKey);
    }

    public byte[] getCertificate() {
        return certificate;
    }

    public void setCertificate(byte[] aCertificate) {
        certificate = aCertificate;
    }

    public byte[] getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(byte[] aPrivateKey) {
        this.privateKey = aPrivateKey;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] aPublicKey) {
        this.publicKey = aPublicKey;
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public String getUrl() {
        return String.format("%s:all", ResourceSchemes.PI_CERT);
    }

    @Override
    public String getUriScheme() {
        return ResourceSchemes.PI_CERT.toString();
    }
}
