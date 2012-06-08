package com.bt.pi.app.common.entities;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.core.entity.Deletable;
import com.bt.pi.core.entity.PiEntityBase;

public class Image extends PiEntityBase implements Deletable {
    private static final String SCHEME = "img";
    private String imageId;
    private String kernelId;
    private String ramdiskId;
    private String manifestLocation;
    private String ownerId;
    private String architecture;
    private boolean isPublic;
    private ImagePlatform platform;
    private ImageState state = ImageState.PENDING;
    private MachineType machineType;
    private boolean deleted;

    public Image() {
    }

    public Image(String anImageId, String aKernelId, String aRamdiskId, String aManifestLocation, String anOwnerId, String anArchitecture, ImagePlatform aPlatform, boolean aIsPublic, MachineType aType) {
        super();
        this.imageId = anImageId;
        this.kernelId = aKernelId;
        this.ramdiskId = aRamdiskId;
        this.manifestLocation = aManifestLocation;
        this.ownerId = anOwnerId;
        this.architecture = anArchitecture;
        this.platform = aPlatform == null ? ImagePlatform.linux : aPlatform;
        this.isPublic = aIsPublic;
        this.machineType = aType;
    }

    public MachineType getMachineType() {
        return machineType;
    }

    public void setMachineType(MachineType aMachineType) {
        machineType = aMachineType;
    }

    public String getArchitecture() {
        return architecture;
    }

    public void setArchitecture(String anArchitecture) {
        this.architecture = anArchitecture;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aIsPublic) {
        this.isPublic = aIsPublic;
    }

    public ImagePlatform getPlatform() {
        return platform;
    }

    public void setPlatform(ImagePlatform aPlatform) {
        this.platform = aPlatform;
    }

    public ImageState getState() {
        return state;
    }

    public void setState(ImageState aState) {
        this.state = aState;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String anImageId) {
        this.imageId = anImageId;
    }

    public String getKernelId() {
        return kernelId;
    }

    public void setKernelId(String aKernelId) {
        this.kernelId = aKernelId;
    }

    public String getManifestLocation() {
        return manifestLocation;
    }

    public void setManifestLocation(String aManifestLocation) {
        this.manifestLocation = aManifestLocation;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String anOwnerId) {
        this.ownerId = anOwnerId;
    }

    public String getRamdiskId() {
        return ramdiskId;
    }

    public void setRamdiskId(String aRamdiskId) {
        this.ramdiskId = aRamdiskId;
    }

    public String getType() {
        return getClass().getSimpleName();
    }

    public String getUrl() {
        return Image.getUrl(imageId);
    }

    public static String getUrl(String entityKey) {
        return String.format(SCHEME + ":%s", entityKey);
    }

    @Override
    public int hashCode() {
        final int HASH_MULTIPLE = 173;
        final int HASH_INITIAL = 7;
        return new HashCodeBuilder(HASH_MULTIPLE, HASH_INITIAL).append(imageId).append(kernelId).append(manifestLocation).append(ownerId).append(ramdiskId).append(architecture).append(isPublic).append(platform).append(state).append(machineType)
                .append(deleted).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Image other = (Image) obj;

        return new EqualsBuilder().append(imageId, other.imageId).append(kernelId, other.kernelId).append(manifestLocation, other.manifestLocation).append(ownerId, other.ownerId).append(ramdiskId, other.ramdiskId)
                .append(architecture, other.architecture).append(isPublic, other.isPublic).append(platform, other.platform).append(state, other.state).append(machineType, other.machineType).append(deleted, other.deleted).isEquals();
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public void setDeleted(boolean b) {
        this.deleted = b;
    }

    @Override
    public String toString() {
        return "Image [architecture=" + architecture + ", deleted=" + deleted + ", imageId=" + imageId + ", isPublic=" + isPublic + ", kernelId=" + kernelId + ", machineType=" + machineType + ", manifestLocation=" + manifestLocation + ", ownerId="
                + ownerId + ", platform=" + platform + ", ramdiskId=" + ramdiskId + ", state=" + state + "]";
    }

    @Override
    public String getUriScheme() {
        return SCHEME;
    }
}
