package com.bt.pi.app.imagemanager.reporting;

import java.util.Date;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.bt.pi.app.common.entities.Image;
import com.bt.pi.core.application.reporter.TimeBasedReportableEntity;

public class ImageReportEntity extends TimeBasedReportableEntity<ImageReportEntity> {
    private Image image;
    private long creationTime;

    public ImageReportEntity() {
        image = null;
        creationTime = new Date().getTime();
    }

    public ImageReportEntity(Image anImage) {
        this();
        this.image = anImage;
    }

    @Override
    public int getKeysForMapCount() {
        return 0;
    }

    @Override
    public Object[] getKeysForMap() {
        return new String[] {};
    }

    @Override
    public int compareTo(ImageReportEntity o) {
        return image.getImageId().compareTo(o.getImage().getImageId());
    }

    @Override
    public String getType() {
        return getClass().getSimpleName();
    }

    @Override
    public String getUriScheme() {
        return getClass().getSimpleName();
    }

    @Override
    public String getUrl() {
        return null;
    }

    @Override
    public Object getId() {
        return image.getImageId();
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof ImageReportEntity))
            return false;
        return image.getImageId().equals(((ImageReportEntity) other).image.getImageId());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(image).toHashCode();
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image anImage) {
        this.image = anImage;
    }
}
