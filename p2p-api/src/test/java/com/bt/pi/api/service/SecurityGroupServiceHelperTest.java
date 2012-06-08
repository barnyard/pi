package com.bt.pi.api.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class SecurityGroupServiceHelperTest {
    private static final String INSTANCE_A = "instanceA";
    private static final String AVAILABILITY_ZONE_1_NAME = "availabilityZone1";
    private static final int AVAILABILITY_ZONE_1_CODE = 10;
    private static final String REGION_ZONE_1_NAME = "Region1";
    private static final int REGION_ZONE_1_CODE = 1;

    @InjectMocks
    SecurityGroupServiceHelper securityGroupServiceHelper = new SecurityGroupServiceHelper();

    @Mock
    ApiApplicationManager apiApplicationManager;
    @Mock
    PiIdBuilder piIdBuilder;
    @Mock
    private PId instanceAPiId;

    Instance instanceA = new Instance();

    @Before
    public void setUp() throws Exception {
        instanceA.setInstanceId(INSTANCE_A);
        instanceA.setState(InstanceState.TERMINATED);

        when(piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id(anyString())).thenReturn(AVAILABILITY_ZONE_1_CODE);

        when(apiApplicationManager.getRegion(REGION_ZONE_1_NAME)).thenReturn(new Region(REGION_ZONE_1_NAME, REGION_ZONE_1_CODE, "bob", ""));
        AvailabilityZones zones = new AvailabilityZones();
        zones.addAvailabilityZone(new AvailabilityZone(AVAILABILITY_ZONE_1_NAME, AVAILABILITY_ZONE_1_CODE, REGION_ZONE_1_CODE, AVAILABILITY_ZONE_1_NAME));
        when(apiApplicationManager.getAvailabilityZonesRecord()).thenReturn(zones);

        when(piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(INSTANCE_A))).thenReturn(instanceAPiId);
    }

    @Test
    public void shouldReturnInstancePiId() {

        // act
        PId instancePiId = securityGroupServiceHelper.getInstancePiId(INSTANCE_A);

        // assert
        assertEquals(instanceAPiId, instancePiId);
    }
}
