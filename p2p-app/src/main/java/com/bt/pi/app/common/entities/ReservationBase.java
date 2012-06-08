package com.bt.pi.app.common.entities;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.core.entity.PiEntityBase;

public abstract class ReservationBase extends PiEntityBase {

    private static final int HASH_MULTIPLE = 61;
    private static final int HASH_INITIAL = 37;

    private String reservationId;
    private String kernelId;
    private String ramdiskId;
    private String imageId;
    private String sourceImagePath;
    private String sourceKernelPath;
    private String sourceRamdiskPath;
    private String keyName;
    private String userId;
    private String securityGroupName;
    private ImagePlatform platform;
    private String additionalInfo;
    private String userData;
    private String instanceType;
    private String availabilityZone;
    private List<BlockDeviceMapping> blockDeviceMappings;
    private boolean monitoring;

    public ReservationBase() {
        this(null);
    }

    public ReservationBase(final ReservationBase reservationBase) {
        blockDeviceMappings = new ArrayList<BlockDeviceMapping>();

        if (reservationBase != null) {
            reservationId = reservationBase.getReservationId();
            kernelId = reservationBase.getKernelId();
            ramdiskId = reservationBase.getRamdiskId();
            imageId = reservationBase.getImageId();
            sourceImagePath = reservationBase.getSourceImagePath();
            sourceKernelPath = reservationBase.getSourceKernelPath();
            sourceRamdiskPath = reservationBase.getSourceRamdiskPath();
            keyName = reservationBase.getKeyName();
            userId = reservationBase.getUserId();
            securityGroupName = reservationBase.getSecurityGroupName();
            platform = reservationBase.getPlatform();
            additionalInfo = reservationBase.getAdditionalInfo();
            userData = reservationBase.getUserData();
            instanceType = reservationBase.getInstanceType();
            availabilityZone = reservationBase.getAvailabilityZone();
            monitoring = reservationBase.getMonitoring();
            for (BlockDeviceMapping bdm : reservationBase.getBlockDeviceMappings())
                blockDeviceMappings.add(bdm);
        }
    }

    public boolean getMonitoring() {
        return monitoring;
    }

    public void setMonitoring(boolean isMonitoring) {
        monitoring = isMonitoring;
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String aReservationId) {
        this.reservationId = aReservationId;
    }

    public List<BlockDeviceMapping> getBlockDeviceMappings() {
        return blockDeviceMappings;
    }

    public void setBlockDeviceMappings(List<BlockDeviceMapping> aBlockDeviceMappings) {
        this.blockDeviceMappings = aBlockDeviceMappings;
    }

    public String getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(String anAdditionalInfo) {
        this.additionalInfo = anAdditionalInfo;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setAvailabilityZone(String anAvailabilityZone) {
        this.availabilityZone = anAvailabilityZone;
    }

    public String getUserData() {
        return userData;
    }

    public void setUserData(String aUserData) {
        this.userData = aUserData;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public void setInstanceType(String anInstanceType) {
        this.instanceType = anInstanceType;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String aUserId) {
        this.userId = aUserId;
    }

    public String getSecurityGroupName() {
        return securityGroupName;
    }

    public void setSecurityGroupName(String aSecurityGroupName) {
        this.securityGroupName = aSecurityGroupName;
    }

    public void setPlatform(ImagePlatform aPlatform) {
        this.platform = aPlatform;
    }

    public ImagePlatform getPlatform() {
        return platform;
    }

    public void setSourceImagePath(String aSourceImagePath) {
        this.sourceImagePath = aSourceImagePath;
    }

    public String getSourceImagePath() {
        return sourceImagePath;
    }

    public void setSourceKernelPath(String aSourceKernelPath) {
        this.sourceKernelPath = aSourceKernelPath;
    }

    public String getSourceKernelPath() {
        return sourceKernelPath;
    }

    public void setSourceRamdiskPath(String aSourceRamdiskPath) {
        this.sourceRamdiskPath = aSourceRamdiskPath;
    }

    public String getSourceRamdiskPath() {
        return sourceRamdiskPath;
    }

    public void setKeyName(String aKeyName) {
        this.keyName = aKeyName;
    }

    public String getKeyName() {
        return keyName;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String anImageId) {
        imageId = anImageId;
    }

    public String getKernelId() {
        return kernelId;
    }

    public void setKernelId(String aKernelId) {
        this.kernelId = aKernelId;
    }

    public String getRamdiskId() {
        return ramdiskId;
    }

    public void setRamdiskId(String aRamdiskId) {
        this.ramdiskId = aRamdiskId;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(HASH_INITIAL, HASH_MULTIPLE).append(userId).append(securityGroupName).append(platform).append(sourceImagePath).append(sourceKernelPath).append(sourceRamdiskPath).append(keyName).append(ramdiskId).append(kernelId)
                .append(imageId).append(additionalInfo).append(userData).append(instanceType).append(availabilityZone).append(blockDeviceMappings).append(reservationId).append(monitoring).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final ReservationBase other = (ReservationBase) obj;
        return new EqualsBuilder().append(userId, other.userId).append(securityGroupName, other.securityGroupName).append(platform, other.platform).append(sourceImagePath, other.sourceImagePath).append(sourceKernelPath, other.sourceKernelPath)
                .append(sourceRamdiskPath, other.sourceRamdiskPath).append(keyName, other.keyName).append(ramdiskId, other.ramdiskId).append(kernelId, other.kernelId).append(imageId, other.imageId).append(additionalInfo, other.additionalInfo)
                .append(userData, other.userData).append(instanceType, other.instanceType).append(availabilityZone, other.availabilityZone).append(blockDeviceMappings, other.blockDeviceMappings).append(reservationId, other.reservationId).append(
                        monitoring, other.monitoring).isEquals();
    }

    @Override
    public String toString() {
        return String
                .format(
                        "[userId=%s,securityGroupName=%s,platform=%s,sourceImagePath=%s,sourceKernelPath=%s,sourceRamdiskPath=%s,keyName=%s,ramdiskId=%s,kernelId=%s,imageId=%s,additionalInfo=%s,userData=%s,instanceType=%s,availabilityZone=%s,blockDeviceMapping=%s,monitoring=%s]",
                        userId, securityGroupName, platform, sourceImagePath, sourceKernelPath, sourceRamdiskPath, keyName, ramdiskId, kernelId, imageId, additionalInfo, userData, instanceType, availabilityZone, blockDeviceMappings, monitoring);
    }

}
