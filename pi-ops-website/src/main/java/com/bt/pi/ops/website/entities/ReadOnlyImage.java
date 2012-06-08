/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website.entities;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.ImageState;
import com.bt.pi.app.common.entities.MachineType;
import com.bt.pi.app.common.images.platform.ImagePlatform;

@XmlRootElement(name="image")
public class ReadOnlyImage {
	private Image image;
	
	public ReadOnlyImage(){}
	
	
	public ReadOnlyImage(Image theimage){
		image = theimage;
	}
	
	public void setimage(Image theimage){
		image=theimage;
	}
	
	

	@XmlElement
    public MachineType getMachineType() {
        return image.getMachineType();
    }

	@XmlElement
    public String getArchitecture() {
        return image.getArchitecture();
    }

	@XmlElement
    public boolean isPublic() {
        return image.isPublic();
    }

	@XmlElement
    public ImagePlatform getPlatform() {
        return image.getPlatform();
    }

	@XmlElement
    public ImageState getState() {
        return image.getState();
    }

	@XmlElement
    public String getImageId() {
        return image.getImageId();
    }

	@XmlElement
    public String getKernelId() {
        return image.getKernelId();
    }

	@XmlElement
    public String getManifestLocation() {
        return image.getManifestLocation();
    }

	@XmlElement
    public String getOwnerId() {
        return image.getOwnerId();
    }

	@XmlElement
    public String getRamdiskId() {
        return image.getRamdiskId();
    }
	
	
	
	
	Image getImage(){
		return image;
	}

    @Override
    public String toString() {
    	return image.toString();
    }
    
    @Override
    public int hashCode() {
    	return image.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
    	Object o;
    	if(obj instanceof ReadOnlyImage){
    		o = ((ReadOnlyImage)obj).getImage();
    	} else {
    		o = obj;
    	}
    	return image.equals(o);
    }
}
