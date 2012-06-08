package com.bt.pi.app.common.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.bt.pi.core.parser.KoalaJsonParser;

public class UserTest {
    private User user;
    private KoalaJsonParser koalaJsonParser;

    @Before
    public void setup() {
        user = new User("username", "access", "secret");
        koalaJsonParser = new KoalaJsonParser();
    }

    @After
    public void after() {
        System.clearProperty("user.max.instances");
        System.clearProperty("user.max.cores");
    }

    @Test
    public void testConstructor() {
        // assert
        assertEquals("access", user.getApiAccessKey());
        assertEquals("secret", user.getApiSecretKey());
        assertEquals("username", user.getUsername());
    }

    @Test
    public void testDefaultContructor() {
        // setup
        user = new User();

        // act
        user.setApiAccessKey("access");
        user.setUsername("username");
        user.setApiSecretKey("secret");

        // assert
        assertEquals("access", user.getApiAccessKey());
        assertEquals("secret", user.getApiSecretKey());
        assertEquals("username", user.getUsername());
    }

    @Test
    public void testUserEquals() {
        // setup
        User user2 = new User("username", "access", "secret");

        // assert
        assertTrue(user.equals(user2));
    }

    @Test
    public void testEqualsWithNull() {
        // act
        boolean result = user.equals(null);

        // assert
        assertFalse(result);
    }

    @Test
    public void testEqualsWithNonUserObject() {
        Instance instance = new Instance();

        // act
        boolean result = user.equals(instance);

        // assert
        assertFalse(result);
    }

    @Test
    public void shouldSetUserInstanceIds() {
        // act
        user.addInstance("i-123");
        user.addInstance("i-456");

        // assert
        assertEquals(2, user.getInstanceIds().length);
    }

    @Test
    public void shouldSetUserVolumeIds() {
        // act
        user.getVolumeIds().add("v-123");
        user.getVolumeIds().add("v-456");

        // assert
        assertEquals(2, user.getVolumeIds().size());
    }

    @Test
    public void shouldSetUserBucketNames() {
        // act
        user.getBucketNames().add("bucket1");
        user.getBucketNames().add("bucket2");

        // assert
        assertEquals(2, user.getBucketNames().size());
    }

    @Test
    public void shouldSetUserSecurityGroups() {
        // act
        user.getSecurityGroupIds().add("default");
        user.getSecurityGroupIds().add("other");

        // assert
        assertEquals(2, user.getSecurityGroupIds().size());
    }

    @Test
    public void shouldSetUserKeyPairs() {
        // act
        user.getKeyPairs().add(new KeyPair("key1"));
        user.getKeyPairs().add(new KeyPair("key2"));

        // assert
        assertEquals(2, user.getKeyPairs().size());
    }

    @Test
    public void testToString() {
        // setup
        user.setRealName("name");
        user.setEnabled(false);
        user.addInstance("i-123");
        user.addInstance("i-456");
        user.getVolumeIds().add("v-456");
        user.getImageIds().add("emi-456");
        user.getKeyPairs().add(new KeyPair("key1"));
        user.getSnapshotIds().add("snap-12345678");

        // act
        String result = user.toString();

        // assert
        assertEquals(
                "[user:[username=username,realName=name,emailAddress=null,isEnabled=false,apiAccessKey=access,apiSecretKey=secret,instanceIds=i-456,i-123,terminatedInstanceIds=,volumeIds=v-456,bucketNames=,certificate=null,imageIds=emi-456,keyPairs=[keyPair:[keyName=key1,keyFingerPrint=null,keyMaterial=null]],snapshotIds=snap-12345678]]",
                result);
    }

    @Test
    public void shouldBeAbleToRoundTripToFromJson() {
        // setup
        user.setCertificate("abcdefg1234".getBytes());

        // act
        String json = koalaJsonParser.getJson(user);
        User reverse = (User) koalaJsonParser.getObject(json, User.class);

        // assert
        assertEquals(user, reverse);
    }

    @Test
    public void shouldBeAbleToRoundTripToFromJsonForNewFields() {
        // setup
        user.setRealName("joe bloggs");
        user.setEmailAddress("joe@bloggs.com");
        user.setEnabled(true);
        user.addTerminatedInstance("abc");

        // act
        String json = koalaJsonParser.getJson(user);
        User reverse = (User) koalaJsonParser.getObject(json, User.class);

        // assert
        assertEquals(user, reverse);
    }

    @Test
    public void testThatMissingListGetsDeserialisedToEmpty() throws Exception {
        // setup
        String json = "{\"type\" : \"User\", \"certificate\" : null, \"apiAccessKey\" : \"access\", \"apiSecretKey\" : \"secret\", \"username\" : \"username\", \"instanceIds\" : [ ],\"volumeIds\" : [ ], \"bucketNames\" : [ ], \"securityGroupIds\" : [ ],\"url\" : \"user:username\", \"realName\" : \"joe bloggs\",\"emailAddress\" : \"joe@bloggs.com\", \"enabled\" : true,\"version\" : 0      }";

        // act
        User result = (User) koalaJsonParser.getObject(json, User.class);

        // assert
        assertEquals(0, result.getImageIds().size());
    }

    @Test
    public void testGetKeyPair() throws Exception {
        // setup
        KeyPair keyPair = new KeyPair("key1");
        user.getKeyPairs().add(keyPair);

        // act
        KeyPair result = user.getKeyPair("key1");

        // assert
        assertEquals(keyPair, result);
    }

    @Test
    public void testGetKeyPairReturnsNullIfNonExistentKeyPair() throws Exception {
        // act
        KeyPair result = user.getKeyPair("key1");

        // assert
        assertNull(result);
    }

    @Test
    public void testGetKeyPairReturnsNullIfKeyNameIsNull() throws Exception {
        // act
        KeyPair result = user.getKeyPair(null);

        // assert
        assertNull(result);
    }

    @Test
    public void shouldStoreExternalRefId() {
        // setup
        final String theExternalRefId = "externalRef";

        // act
        user.setExternalRefId(theExternalRefId);

        // assert
        assertEquals(theExternalRefId, user.getExternalRefId());
    }

    @Test
    public void shouldStoreAllDetailsOnBigConstructor() {
        // setup
        final String theExternalRefId = "external ref";
        final boolean isEnabled = true;
        final String anEmailAddress = "email";
        final String username = "username";
        final String realName = "real name";

        // act
        User user = new User(username, realName, anEmailAddress, isEnabled, theExternalRefId);

        // assert
        assertEquals(username, user.getUsername());
        assertEquals(realName, user.getRealName());
        assertEquals(isEnabled, user.isEnabled());
        assertEquals(anEmailAddress, user.getEmailAddress());
        assertEquals(theExternalRefId, user.getExternalRefId());
    }

    @Test
    public void defaultConstructorShouldSetDefaultMaxInstances() {
        // setup

        // act
        User result = new User();

        // assert
        assertEquals(Integer.parseInt(User.DEFAULT_MAX_INSTANCES), result.getMaxInstances());
    }

    @Test
    public void defaultConstructorShouldSetDefaultMaxCores() {
        // setup

        // act
        User result = new User();

        // assert
        assertEquals(Integer.parseInt(User.DEFAULT_MAX_CORES), result.getMaxCores());
    }

    @Test
    public void defaultConstructorShouldSetDefaultMaxInstancesFromSystemPropertyIfItExists() {
        // setup
        System.setProperty("user.max.instances", "48");

        // act
        User result = new User();

        // assert
        assertEquals(48, result.getMaxInstances());
    }

    @Test
    public void defaultConstructorShouldSetDefaultMaxCoresFromSystemPropertyIfItExists() {
        // setup
        System.setProperty("user.max.cores", "48");

        // act
        User result = new User();

        // assert
        assertEquals(48, result.getMaxCores());
    }

    @Test
    public void shouldRoundTripMaxInstances() throws Exception {
        // setup
        user.setMaxInstances(23);
        assertEquals(23, user.getMaxInstances());

        // act
        String json = koalaJsonParser.getJson(user);
        User reverse = (User) koalaJsonParser.getObject(json, User.class);

        // assert
        assertEquals(23, reverse.getMaxInstances());
    }

    @Test
    public void shouldRoundTripMaxCores() throws Exception {
        // setup
        user.setMaxCores(23);
        assertEquals(23, user.getMaxCores());

        // act
        String json = koalaJsonParser.getJson(user);
        User reverse = (User) koalaJsonParser.getObject(json, User.class);

        // assert
        assertEquals(23, reverse.getMaxCores());
    }

    @Test
    public void shouldReturnDefaultMaxInstancesIfNotInJson() {
        // setup
        String json = "{\"type\" : \"User\",\"instanceIds\" : [ ],\"volumeIds\" : [ ],\"bucketNames\" : [ ],\"securityGroupIds\" : [ ],\"keyPairs\" : [ ],\"imageIds\" : [ ],\"snapshotIds\" : [ ],\"enabled\" : false,\"url\" : \"user:null\",\"deleted\" : false,\"uriScheme\" : \"user\",\"version\" : 0}";

        // act
        User result = (User) koalaJsonParser.getObject(json, User.class);

        // assert
        assertEquals(Integer.parseInt(User.DEFAULT_MAX_INSTANCES), result.getMaxInstances());
    }

    @Test
    public void shouldReturnDefaultMaxCoresIfNotInJson() {
        // setup
        String json = "{\"type\" : \"User\",\"instanceIds\" : [ ],\"volumeIds\" : [ ],\"bucketNames\" : [ ],\"securityGroupIds\" : [ ],\"keyPairs\" : [ ],\"imageIds\" : [ ],\"snapshotIds\" : [ ],\"enabled\" : false,\"url\" : \"user:null\",\"deleted\" : false,\"uriScheme\" : \"user\",\"version\" : 0}";

        // act
        User result = (User) koalaJsonParser.getObject(json, User.class);

        // assert
        assertEquals(Integer.parseInt(User.DEFAULT_MAX_CORES), result.getMaxCores());
    }

    @Test
    public void shouldNotPersistMaxInstancesIfNotSet() {
        // setup
        assertEquals(Integer.parseInt(User.DEFAULT_MAX_INSTANCES), user.getMaxInstances());

        // act
        String json = koalaJsonParser.getJson(user);

        // assert
        assertFalse(json.contains("maxInstances"));
    }

    @Test
    public void shouldNotPersistMaxCoresIfNotSet() {
        // setup
        assertEquals(Integer.parseInt(User.DEFAULT_MAX_CORES), user.getMaxCores());

        // act
        String json = koalaJsonParser.getJson(user);

        // assert
        assertFalse(json.contains("maxCores"));
    }

    @Test
    public void shouldTerminateAnInstance() {
        // setup
        String instanceId = "abc123";
        user.addInstance(instanceId);

        // act
        boolean result = user.terminateInstance(instanceId);

        // assert
        assertTrue(result);
        assertEquals(0, user.getInstanceIds().length);
        assertEquals(1, user.getTerminatedInstanceIds().size());
        assertTrue(user.getTerminatedInstanceIds().contains(instanceId));
    }

    @Test
    public void shouldTerminateAnInstanceWithType() {
        // setup
        String instanceId = "abc123";
        user.addInstance(instanceId + ";xlarge");

        // act
        boolean result = user.terminateInstance(instanceId);

        // assert
        assertTrue(result);
        assertEquals(0, user.getInstanceIds().length);
        assertEquals(1, user.getTerminatedInstanceIds().size());
        assertTrue(user.getTerminatedInstanceIds().contains(instanceId));
    }

    private boolean arrayContains(String[] array, String target) {
        for (String s : array)
            if (s.equals(target))
                return true;
        return false;
    }

    @Test
    public void shouldNotTerminateANonExistantInstance() {
        // setup
        String instanceId = "abc123";
        user.addInstance(instanceId);

        // act
        boolean result = user.terminateInstance("fred");

        // assert
        assertTrue(result);
        assertEquals(1, user.getInstanceIds().length);
        assertTrue(arrayContains(user.getInstanceIds(), instanceId));
        assertEquals(1, user.getTerminatedInstanceIds().size());
        assertTrue(user.getTerminatedInstanceIds().contains("fred"));
    }

    @Test
    public void shouldRemoveInstance() {
        // setup
        String instanceId = "abc123";
        user.addInstance(instanceId);

        // act
        boolean result = user.removeInstance(instanceId);

        // assert
        assertTrue(result);
        assertEquals(0, user.getInstanceIds().length);
        assertEquals(0, user.getTerminatedInstanceIds().size());
    }

    @Test
    public void shouldRemoveTerminatedInstance() {
        // setup
        String instanceId = "abc123";
        user.addTerminatedInstance(instanceId);

        // act
        boolean result = user.removeInstance(instanceId);

        // assert
        assertTrue(result);
        assertEquals(0, user.getInstanceIds().length);
        assertEquals(0, user.getTerminatedInstanceIds().size());
    }

    @Test
    public void shouldRemoveInstanceFromBoth() {
        // setup
        String instanceId = "abc123";
        user.addInstance(instanceId);
        user.addTerminatedInstance(instanceId);

        // act
        boolean result = user.removeInstance(instanceId);

        // assert
        assertTrue(result);
        assertEquals(0, user.getInstanceIds().length);
        assertEquals(0, user.getTerminatedInstanceIds().size());
    }

    @Test
    public void shouldRemoveInstanceWithTypeFromBoth() {
        // setup
        String instanceId = "abc123";
        String instanceIdWithType = instanceId + ";fred";
        user.addInstance(instanceIdWithType);
        user.addTerminatedInstance(instanceIdWithType);

        // act
        boolean result = user.removeInstance(instanceId);

        // assert
        assertTrue(result);
        assertEquals(0, user.getInstanceIds().length);
        assertEquals(0, user.getTerminatedInstanceIds().size());
    }

    @Test
    public void shouldNotRemoveInstance() {
        // setup
        String instanceId = "abc123";
        user.addInstance(instanceId);

        // act
        boolean result = user.removeInstance("fred");

        // assert
        assertFalse(result);
        assertEquals(1, user.getInstanceIds().length);
        assertTrue(arrayContains(user.getInstanceIds(), instanceId));
        assertEquals(0, user.getTerminatedInstanceIds().size());
    }

    @Test
    public void shouldDeserializeOldFormat() {
        // setup
        String json = "{"
                + "\"maxInstances\" : 30,"
                + "\"type\" : \"User\","
                + "\"certificate\" : \"MIIDeTCCAmGgAwIBAgIGAS4KujhdMA0GCSqGSIb3DQEBDQUAMHMxCzAJBgNVBAYTAlVLMRAwDgYDVQQIEwdFbmdsYW5kMQ8wDQYDVQQHEwZMb25kb24xGDAWBgNVBAoTD3JvYnVzdG5lc3NidWlsZDELMAkGA1UECxMCUGkxGjAYBgNVBAMTEXd3dy5jbG91ZDIxY24uY29tMB4XDTExMDIwOTE0MDEzOVoXDTE2MDIwOTE0MDEzOVowczELMAkGA1UEBhMCVUsxEDAOBgNVBAgTB0VuZ2xhbmQxDzANBgNVBAcTBkxvbmRvbjEYMBYGA1UEChMPcm9idXN0bmVzc2J1aWxkMQswCQYDVQQLEwJQaTEaMBgGA1UEAxMRd3d3LmNsb3VkMjFjbi5jb20wggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCwF2os9L5lylu0VJPj0tmYBBtWw6vm3dVn+U1V3KLVzzExRL+kEKlkKaPl0AgEHVZiQre4H95qjyqyOV6GmG20ZCtJFjXZBDvLm8AFKEMN20IdYy7XhZm0w+KiQ6wu5uPyZcnTnDDRd2JqHETQ1aspC14zGcIepXaSQAjXq88sH+fCtwdBItqgFR6mrhBdBiBtgNCmbEqOu5lnrGYMCMTQCJgjT/5yCFaWeyCmKOUeHFUD1LDoj1+8GXAtDqiH0OcSLQl6ZLVUZiUrSbsP8lOcCWW8VxUJMkPjQZo3jlT/4el3OqOCfweVQ4U5bQD216+8dMXdxRGg/IDalIKOa7GpAgMBAAGjEzARMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQENBQADggEBACTGz0oNeq8dISPIQ88DA3l8dT6sY/1IdDscddb23v56eLuUC9sUzs2L/Hf8ioxmjOE0g32C5EkUPzOqoeSQu1mRVEVfu21VQ15hIGAYln/3hAf56oX96TCtxZ/5hZX7Nl44c3gFvbfqtz5URaihksn7HiygOaenI6X8nZ0aG0qjqJpmS03fZc6y7SCoglD/1slqAhoiBlD03cSUVjsBnkkjvxVXUXusP/WAVVvBfHBs3gp0NfrLPg0o8d09jZfCN5Wmtz53ulAmrGrtfrHwXCKmhgM2boOwN0a7bTq3q5V5sQf5Q+AaYZYOKG5kWCauRQ9U2f/+Rq28bdrMj9+f1aA=\","
                + "\"uriScheme\" : \"user\"," + "\"url\" : \"user:robustnessbuild\"," + "\"enabled\" : true," + "\"username\" : \"robustnessbuild\"," + "\"snapshotIds\" : [" + "    \"snap-0003zXei\"" + "]," + "\"volumeIds\" : ["
                + "    \"vol-000Bt8wY\"," + "    \"vol-000GmFNz\"" + "]," + "\"imageIds\" : []," + "\"instanceIds\" : [\"abc\",\"def\"]," + "\"terminatedInstanceIds\" : [ \"i-000Dj4f2\"," + "   \"i-000361E3\"," + "   \"i-0008fBxE\","
                + "   \"i-0001pSbC\"" + "]," + "\"deleted\" : false," + "\"keyPairs\" : [" + "]," + "\"securityGroupIds\" : [" + "    \"default\"" + "]," + "\"apiAccessKey\" : \"yKH2R8TAOmmzTL2wocfy\","
                + "\"apiSecretKey\" : \"GY9c_k1AWhpgllhtnIWM560aqQXzKtEP2iuI4Q..\"," + "\"bucketNames\" : [" + "]," + "\"realName\" : \"Robustness User\"," + "\"emailAddress\" : \"robustness@cloud21cn.com\"," + "\"version\" : 82783" + "}";

        // act
        User result = (User) koalaJsonParser.getObject(json, User.class);

        // assert
        assertEquals(2, result.getInstanceIds().length);
        assertEquals(4, result.getTerminatedInstanceIds().size());
    }

    @Test
    public void addInstanceWithTypeShouldBeParsed() {
        // setup
        String instanceId = "abc123;x.large";
        user.addInstance(instanceId);

        // act
        String[] result = user.getInstanceIds();

        // assert
        assertEquals(1, result.length);
        assertEquals("abc123", result[0]);
    }

    @Test
    public void testGetInstanceTypes() {
        // setup
        String instanceId1 = "abc123;x.large";
        user.addInstance(instanceId1);
        String instanceId2 = "def456";
        user.addInstance(instanceId2, "m.small");

        // act
        String[] instanceTypes = user.getInstanceTypes();

        // assert
        assertEquals(2, instanceTypes.length);
        assertTrue(arrayContains(instanceTypes, "x.large"));
        assertTrue(arrayContains(instanceTypes, "m.small"));
    }

    @Test
    public void testGetInstanceTypesWithUnknown() {
        // setup
        String instanceId1 = "abc123;x.large";
        user.addInstance(instanceId1);
        String instanceId2 = "def456";
        user.addInstance(instanceId2);

        // act
        String[] instanceTypes = user.getInstanceTypes();

        // assert
        assertEquals(2, instanceTypes.length);
        assertTrue(arrayContains(instanceTypes, "x.large"));
        assertTrue(arrayContains(instanceTypes, InstanceTypes.UNKNOWN));
    }

    @Test
    public void testHasInstanceTrue() {
        // setup
        String instanceId = "abc123";
        user.addInstance(instanceId);

        // act
        boolean result = user.hasInstance(instanceId);

        // assert
        assertTrue(result);
    }

    @Test
    public void testHasInstanceFalse() {
        // setup
        String instanceId = "abc123";
        user.addInstance(instanceId);

        // act
        boolean result = user.hasInstance(instanceId + "1");

        // assert
        assertFalse(result);
    }

    @Test
    public void testHasInstanceWithTypeTrue() {
        // setup
        String instanceId = "abc123;x.large";
        user.addInstance(instanceId);

        // act
        boolean result = user.hasInstance("abc123");

        // assert
        assertTrue(result);
    }

    @Test
    public void testHasInstanceWithTypeFalse() {
        // setup
        String instanceId = "abc123;x.large";
        user.addInstance(instanceId);

        // act
        boolean result = user.hasInstance("abc1231");

        // assert
        assertFalse(result);
    }
}
