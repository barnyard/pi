package com.bt.pi.app.common.id;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigInteger;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.BucketMetaData;
import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.MachineType;
import com.bt.pi.app.common.entities.OwnerIdGroupNamePair;
import com.bt.pi.app.common.entities.Regions;
import com.bt.pi.app.common.entities.ResourceSchemes;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.app.common.util.Base62Utils;
import com.bt.pi.core.entity.Locatable;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;

public class PiIdBuilderTest {

    private static final String INSTANCE_ID = "instanceId";
    private PiIdBuilder piIdBuilder;
    private KoalaIdFactory koalaIdFactory;
    private Instance instance;
    private PId id;
    private SecurityGroup securityGroup;

    @Before
    public void setUp() throws Exception {
        instance = new Instance();
        instance.setInstanceId(INSTANCE_ID);
        securityGroup = new SecurityGroup();
        securityGroup.setOwnerIdGroupNamePair(new OwnerIdGroupNamePair("owner", "default"));

        koalaIdFactory = mock(KoalaIdFactory.class);
        when(koalaIdFactory.getRegion()).thenReturn(99);
        piIdBuilder = new PiIdBuilder();
        piIdBuilder.setKoalaIdFactory(koalaIdFactory);
        id = mock(PId.class);
    }

    @Test
    public void shouldGetInstanceIdFromInstance() {
        // setup
        BigInteger randomIdPart = new BigInteger("56789AB", 16);
        String instanceId = "i-" + Base62Utils.encodeToBase62(piIdBuilder.addGlobalAvailabilityZoneCodeToNumber(0x1234, randomIdPart));

        instance.setInstanceId(instanceId);
        when(koalaIdFactory.buildPId(isA(Locatable.class))).thenReturn(id);
        when(id.forGlobalAvailablityZoneCode(eq(0x1234))).thenReturn(id);

        // act
        PId id2 = piIdBuilder.getPIdForEc2AvailabilityZone(instance);

        // assert
        assertNotNull(id2);
        assertEquals(id, id2);
    }

    @Test
    public void shouldGetPIdForLocatable() {
        // setup

        // act
        Image image = new Image("hello", null, null, null, null, null, ImagePlatform.linux, true, MachineType.KERNEL);
        piIdBuilder.getPId(image);

        // assert
        verify(koalaIdFactory).buildPId(eq(image));
    }

    @Test
    public void shouldgetPIdForUrl() {
        // act
        piIdBuilder.getPId("img:mirror");

        // assert
        verify(koalaIdFactory).buildPId("img:mirror");
    }

    @Test
    public void getPiIdForEc2StringForTheEncodedAvz() {
        // setup
        BigInteger randomIdPart = new BigInteger("56789AB", 16);
        String instanceId = "i-" + Base62Utils.encodeToBase62(piIdBuilder.addGlobalAvailabilityZoneCodeToNumber(0x1234, randomIdPart));
        PId mockPid = mock(PId.class);
        when(koalaIdFactory.buildPId(Instance.getUrl(instanceId))).thenReturn(mockPid);
        when(mockPid.forGlobalAvailablityZoneCode(0x1234)).thenReturn(mockPid);

        // act
        PId id = piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));

        // verify
        assertEquals(mockPid, id);

    }

    /*
    @Test
    public void testGetQueueIdFromPiQueue() {
        // setup
        PiQueue queue = PiQueue.CREATE_VOLUME;
        when(koalaIdFactory.buildIdLocal(eq(queue.getPiLocation()), eq(TaskProcessingQueue.TYPE_CODE))).thenReturn(id);

        // act
        Id result = this.piIdBuilder.getLocalQueueIdFromPiQueue(PiQueue.CREATE_VOLUME);

        // assert
        assertEquals(id, result);
    }

        
    @Test
    public void testGetQueueIdFromPiQueueName() {
        // setup
        PiQueue queue = PiQueue.CREATE_VOLUME;
        when(koalaIdFactory.buildIdLocal(eq(queue.getPiLocation()), eq(TaskProcessingQueue.TYPE_CODE))).thenReturn(id);

        // act
        Id result = this.piIdBuilder.getLocalQueueIdFromPiQueueName(PiQueue.CREATE_VOLUME.toString());

        // assert
        assertEquals(id, result);
    }

    @Test
    public void testGetQueueIdFromPiQueueForRegion() {
        // setup
        PiQueue queue = PiQueue.ASSOCIATE_ADDRESS;
        when(koalaIdFactory.buildIdForGlobalAvailabilityZoneCode(queue.getPiLocation(), 0x3300, TaskProcessingQueue.TYPE_CODE)).thenReturn(id);

        // act
        Id result = this.piIdBuilder.getQueueIdFromPiQueueForRegion(queue, 0x33);

        // assert
        assertEquals(id, result);
            }*/

    /*
    @Test
    public void testGetQueueIdFromPiQueueForAvz() {
        // setup
        PiQueue queue = PiQueue.CREATE_VOLUME;
            when(koalaIdFactory.buildPId(queue.getPiLocation(), 0x3322, TaskProcessingQueue.TYPE_CODE)).thenReturn(id);

        // act
            PId result = this.piIdBuilder.getQueueIdFromPiQueueForGlobalAvailabilityZoneCode(queue, 0x3322);

        // assert
        assertEquals(id, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailToGetQueueIdFromPiQueueForAvzWhenQueueHasIncorrectScope() {
        // setup
        PiQueue queue = PiQueue.ASSOCIATE_ADDRESS;

        // act
        this.piIdBuilder.getQueueIdFromPiQueueForGlobalAvailabilityZoneCode(queue, 0x3322);
    }
    */
    @Test
    public void testGetRegionsId() {
        // setup
        when(koalaIdFactory.buildPId(new Regions().getUrl())).thenReturn(id);

        // act
        PId result = piIdBuilder.getRegionsId();

        // assert
        assertEquals(id, result);
    }

    @Test
    public void testGetAvailabilityZonesId() {
        // setup
        when(koalaIdFactory.buildPId(new AvailabilityZones().getUrl())).thenReturn(id);

        // act
        PId result = piIdBuilder.getAvailabilityZonesId();

        // assert
        assertEquals(id, result);
    }

    @Test
    public void testGetNodeId() {
        // setup
        String nodeId = "beans R Cool Fool!";

        // act
        piIdBuilder.getNodeIdFromNodeId(nodeId);

        verify(koalaIdFactory).buildPIdFromHexString(nodeId);
    }

    @Test
    public void testGetBucketIdFromBucketName() {
        // setup
        String bucketName = "bucket1";

        // act
        piIdBuilder.getPId(BucketMetaData.getUrl(bucketName.toLowerCase(Locale.getDefault())));

        // assert
        verify(koalaIdFactory).buildPId(ResourceSchemes.BUCKET_META_DATA + ":" + bucketName);
    }

    @Test
    public void testGetBucketIdFromBucketNameWithSomeUpperCase() {
        // setup
        String bucketName = "bucket1ABCD";

        // act
        piIdBuilder.getPId(BucketMetaData.getUrl(bucketName.toLowerCase(Locale.getDefault())));

        // assert
        verify(koalaIdFactory).buildPId(ResourceSchemes.BUCKET_META_DATA + ":" + bucketName.toLowerCase());
    }

    @Test
    public void testGetAvailabilityZoneFromEc2Id() throws Exception {
        // setup
        BigInteger madeUpId = new BigInteger("123456789AB", 16);
        String base62Id = Base62Utils.encodeToBase62(madeUpId.toByteArray());

        // act
        int avz = this.piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id("i-" + base62Id);

        // assert
        assertEquals(0x1234, avz);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAvailabilityZoneFromMadeUpEc2IdOutOfRange() throws Exception {
        // act
        this.piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id("i-instance");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAvailabilityZoneFromEc2IdJustOutOfRange() throws Exception {
        // act
        this.piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id("i-" + Base62Utils.encodeToBase62(new BigInteger("100000000000", 16)));
    }

    @Test
    public void testGetAvailabilityZoneFromEc2IdJustWithinRange() throws Exception {
        // act
        int avz = this.piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id("i-" + Base62Utils.encodeToBase62(new BigInteger("FFFFFFFFFFF", 16)));

        // assert
        assertTrue(avz >= 0);
        assertTrue(avz <= 0xFFFFL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailToGetGetAvailabilityZoneFromNonHyphenatedEc2Id() throws Exception {
        // act
        this.piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id("nohyphen01234567");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFailToGetGetAvailabilityZoneFromBadEc2Id() throws Exception {
        // act
        this.piIdBuilder.getGlobalAvailabilityZoneCodeFromEc2Id("i-waytooloooooooooong");
    }

    @Test
    public void testGetAvailabilityZoneFromBase62() throws Exception {
        // setup
        BigInteger madeUpId = new BigInteger("123456789AB", 16);
        String base62Id = Base62Utils.encodeToBase62(madeUpId.toByteArray());

        // act
        int avz = this.piIdBuilder.getGlobalAvailabilityZoneCodeFromBase62Number(base62Id);

        // assert
        assertEquals(0x1234, avz);
    }

    @Test
    public void testGetAvailabilityZoneFromBigBase62Id() throws Exception {
        // setup
        BigInteger madeUpId = new BigInteger("FFFFAABCDEF", 16);
        String base62Id = Base62Utils.encodeToBase62(madeUpId.toByteArray());

        // act
        int avz = this.piIdBuilder.getGlobalAvailabilityZoneCodeFromBase62Number(base62Id);

        // assert
        assertEquals(0xFFFF, avz);
    }

    @Test
    public void testGetAvailabilityZoneFromSmallBase62Id() throws Exception {
        // setup
        BigInteger madeUpId = new BigInteger("20000001", 16);
        String base62Id = Base62Utils.encodeToBase62(madeUpId.toByteArray());

        // act
        int avz = this.piIdBuilder.getGlobalAvailabilityZoneCodeFromBase62Number(base62Id);
        // assert
        assertEquals(2, avz);
    }

    @Test
    public void testGetAvailabilityZoneFromZeroBase62Id() throws Exception {
        // act
        int avz = this.piIdBuilder.getGlobalAvailabilityZoneCodeFromBase62Number("0");

        // assert
        assertEquals(0, avz);
    }

    @Test
    public void shouldGenerateStandardEc2Id() {
        // act
        String res = piIdBuilder.generateStandardEc2Id("p");

        // assert
        assertTrue(res.startsWith("p-"));
        assertEquals(10, res.length());
        assertTrue(0 <= new BigInteger(res.substring(2), 16).longValue());
        assertTrue(0xFFFFFFFFL >= new BigInteger(res.substring(2), 16).longValue());
    }

    @Test
    public void shouldGenerateEc2IdForGlobalAvzCode() {
        // act
        String res = piIdBuilder.generateBase62Ec2Id("p", 0x1234);

        // assert
        assertTrue(res.startsWith("p-"));

        // validate overall id size and range
        BigInteger id = Base62Utils.decodeBase62(res.substring(2));
        assertEquals(10, res.length());
        assertTrue(0 <= id.longValue());
        assertTrue(0xFFFFFFFFFFFL >= id.longValue());

        // validate id length in hex / bits
        assertEquals(11, id.toString(16).length());

        // assert avz
        assertEquals("1234", id.toString(16).substring(0, 4));

        // validate range of id without avz
        assertTrue(0 <= new BigInteger(id.toString(16).substring(4), 16).longValue());
        assertTrue(0xFFFFFFF >= new BigInteger(id.toString(16).substring(4), 16).longValue());
    }

    @Test
    public void shouldGenerateEc2IdForLargeGlobalAvzCode() {
        // act
        String res = piIdBuilder.generateBase62Ec2Id("p", 0xFFFF);
        // assert
        assertTrue(res.startsWith("p-"));
        assertEquals(10, res.length());

        BigInteger id = Base62Utils.decodeBase62(res.substring(2));
        assertEquals("ffff", id.toString(16).substring(0, 4));
    }

    @Test
    public void shouldGenerateEc2IdForZeroGlobalAvzCode() {
        // act
        String res = piIdBuilder.generateBase62Ec2Id("p", 0);

        // assert
        assertTrue(res.startsWith("p-"));
        assertEquals(10, res.length());

        BigInteger id = Base62Utils.decodeBase62(res.substring(2));
        assertEquals("0000", StringUtils.leftPad(id.toString(16), 11, '0').substring(0, 4));
    }

}
