/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.app.imagemanager.xml;

import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class Manifest {
    private String encryptedKey;
    private String encryptedIV;
    private String signature;
    private String machineConfiguration;
    private String image;
    private List<String> partFilenames;
    private String arch;
    private String kernelId;
    private String ramdiskId;

    public Manifest(String theEncryptedKey, String theEncryptedIV, String theSignature, String theMachineConfiguration, String theImage, List<String> thePartFilenames, String aArch, String aKernelId, String aRamdiskId) {
        this.encryptedKey = theEncryptedKey;
        this.encryptedIV = theEncryptedIV;
        this.signature = theSignature;
        this.machineConfiguration = theMachineConfiguration;
        this.image = theImage;
        this.partFilenames = thePartFilenames;
        this.arch = aArch;
        this.kernelId = aKernelId;
        this.ramdiskId = aRamdiskId;
    }

    public String getEncryptedKey() {
        return encryptedKey;
    }

    public String getEncryptedIV() {
        return encryptedIV;
    }

    public String getSignature() {
        return signature;
    }

    public String getMachineConfiguration() {
        return machineConfiguration;
    }

    public String getImage() {
        return image;
    }

    public List<String> getPartFilenames() {
        return partFilenames;
    }

    public String getArch() {
        return arch;
    }

    public String getKernelId() {
        return kernelId;
    }

    public String getRamdiskId() {
        return ramdiskId;
    }

    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof Manifest))
            return false;
        Manifest castOther = (Manifest) other;
        return new EqualsBuilder().append(encryptedKey, castOther.encryptedKey).append(encryptedIV, castOther.encryptedIV).append(signature, castOther.signature).append(machineConfiguration, castOther.machineConfiguration).append(image,
                castOther.image).append(partFilenames, castOther.partFilenames).append(arch, castOther.arch).append(kernelId, castOther.kernelId).append(ramdiskId, castOther.ramdiskId).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(encryptedKey).append(encryptedIV).append(signature).append(machineConfiguration).append(image).append(partFilenames).append(arch).append(kernelId).append(ramdiskId).toHashCode();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).append("encryptedKey", encryptedKey).append("encryptedIV", encryptedIV).append("signature", signature).append("machineConfiguration", machineConfiguration).append("image",
                image).append("partFilenames", partFilenames).append("arch", arch).append("kernelId", kernelId).append("ramdiskId", ramdiskId).toString();
    }
}
