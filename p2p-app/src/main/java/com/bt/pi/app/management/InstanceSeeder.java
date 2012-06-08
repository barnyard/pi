package com.bt.pi.app.management;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.InstanceTypeConfiguration;
import com.bt.pi.app.common.entities.InstanceTypes;
import com.bt.pi.core.id.PId;

@Component
public class InstanceSeeder extends SeederBase {

    private static final Log LOG = LogFactory.getLog(InstanceSeeder.class);

    public InstanceSeeder() {
        super();
    }

    public boolean configureInstanceTypes(String instanceTypeNames, String numCores, String memorySizesInMB, String diskSizesInGB) {
        LOG.info(String.format("Creating instance types for types %s, with number of cores %s, memory %s MB, disk %s GB", instanceTypeNames, numCores, memorySizesInMB, diskSizesInGB));

        String[] instanceTypeNamesArray = instanceTypeNames.split(SEMICOLON);
        String[] numCoresArray = numCores.split(SEMICOLON);
        String[] memorySizesArray = memorySizesInMB.split(SEMICOLON);
        String[] diskSizesArray = diskSizesInGB.split(SEMICOLON);

        if (!(instanceTypeNamesArray.length == numCoresArray.length && instanceTypeNamesArray.length == memorySizesArray.length && instanceTypeNamesArray.length == diskSizesArray.length))
            return false;

        InstanceTypes instanceTypes = new InstanceTypes();
        for (int i = 0; i < instanceTypeNamesArray.length; i++) {
            InstanceTypeConfiguration instanceTypeConfiguration = new InstanceTypeConfiguration(instanceTypeNamesArray[i], Integer.parseInt(numCoresArray[i]), Integer.parseInt(memorySizesArray[i]), Integer.parseInt(diskSizesArray[i]));
            instanceTypes.addInstanceType(instanceTypeConfiguration);
        }

        PId instanceTypeId = getPiIdBuilder().getPId(instanceTypes);
        boolean instanceTypeRecordWritten = getDhtClientFactory().createBlockingWriter().writeIfAbsent(instanceTypeId, instanceTypes);
        LOG.info(String.format("Instance types record %s created", instanceTypeRecordWritten ? "" : NOT));
        return instanceTypeRecordWritten;
    }
}
