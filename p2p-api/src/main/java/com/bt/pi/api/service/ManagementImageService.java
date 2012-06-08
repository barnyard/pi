/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import com.bt.pi.app.common.entities.MachineType;

public interface ManagementImageService extends ImageService {
    // this method is only for use by the DeleteUserHelper and allows it to de-register images without knowing the
    // MachineType. Note that it isn't on the ImageService interface
    boolean deregisterImageWithoutMachineTypeCheck(final String ownerId, final String imageId);

    boolean deregisterImage(String userId, String imageId, MachineType machineType);

    String registerImage(String userId, String imageManifestLocation, MachineType machineType);
}
