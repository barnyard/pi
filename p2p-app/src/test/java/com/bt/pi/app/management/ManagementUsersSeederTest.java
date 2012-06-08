/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.app.management;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.app.common.entities.ManagementRoles;
import com.bt.pi.app.common.entities.ManagementUser;
import com.bt.pi.app.common.entities.ManagementUsers;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.testing.UpdateResolverAnswer;

@RunWith(MockitoJUnitRunner.class)
public class ManagementUsersSeederTest {
    private static final String PASSWORD = "password";
    private static final String USERNAME = "username";
    private ManagementUsersSeeder seeder;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private PId id;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private BlockingDhtReader reader;
    @Mock
    private BlockingDhtWriter writer;
    private ManagementUser user;

    @Before
    public void doBefore() {
        seeder = new ManagementUsersSeeder();
        this.seeder.setPiIdBuilder(this.piIdBuilder);
        this.seeder.setDhtClientFactory(this.dhtClientFactory);
        when(dhtClientFactory.createBlockingReader()).thenReturn(reader);
        when(dhtClientFactory.createBlockingWriter()).thenReturn(writer);
        when(piIdBuilder.getPId(isA(ManagementUsers.class))).thenReturn(id);

        user = new ManagementUser();
        user.setUsername(USERNAME);
        user.setPassword(PASSWORD);
        user.getRoles().add(ManagementRoles.ROLE_MIS);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void addUserShouldCreateANewManagementUsersIfItDoesntExist() {
        // setup
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver<ManagementUsers> ur = (UpdateResolver<ManagementUsers>) invocation.getArguments()[2];
                ManagementUsers newUsers = ur.update(null, null);
                assertEquals(1, newUsers.getUserMap().size());
                ManagementUser managementUser = newUsers.getUserMap().get(USERNAME);
                assertEquals(USERNAME, managementUser.getUsername());
                assertEquals(PASSWORD, managementUser.getPassword());
                assertEquals(1, managementUser.getRoles().size());
                return null;
            }
        }).when(writer).update(eq(id), (PiEntity) isNull(), isA(UpdateResolver.class));

        // act
        seeder.addUser(USERNAME, PASSWORD, ";ROLE_MIS".split(";"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void addUserShouldUseTheExistingManagementUsersIfItExists() {
        // setup
        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver<ManagementUsers> ur = (UpdateResolver<ManagementUsers>) invocation.getArguments()[2];
                ManagementUsers existingUsers = new ManagementUsers();
                ManagementUser aUser = new ManagementUser();
                aUser.setUsername("UserA");
                aUser.setPassword("PasswordA");

                existingUsers.getUserMap().put("UserA", aUser);

                ManagementUsers newUsers = ur.update(existingUsers, null);
                assertEquals(2, newUsers.getUserMap().size());
                assertEquals("UserA", newUsers.getUserMap().get("UserA").getUsername());
                assertEquals("PasswordA", newUsers.getUserMap().get("UserA").getPassword());

                assertEquals(USERNAME, newUsers.getUserMap().get(USERNAME).getUsername());
                assertEquals(PASSWORD, newUsers.getUserMap().get(USERNAME).getPassword());

                return null;
            }
        }).when(writer).update(eq(id), (PiEntity) isNull(), isA(UpdateResolver.class));

        // act
        seeder.addUser(USERNAME, PASSWORD, ManagementRoles.ROLE_MIS.name());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldAddMultipleRolesToManagementUser() {
        // setup
        when(piIdBuilder.getPId(isA(ManagementUsers.class))).thenReturn(id);

        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                UpdateResolver<ManagementUsers> ur = (UpdateResolver<ManagementUsers>) invocation.getArguments()[2];
                ManagementUsers newUsers = ur.update(null, null);
                assertEquals(1, newUsers.getUserMap().size());
                assertEquals(3, newUsers.getUserMap().get(USERNAME).getRoles().size());
                return null;
            }
        }).when(writer).update(eq(id), (PiEntity) isNull(), isA(UpdateResolver.class));

        // act
        seeder.addUser(USERNAME, PASSWORD, ManagementRoles.ROLE_MIS.name(), ManagementRoles.ROLE_PROVISIONING.name(), ManagementRoles.ROLE_OPS.name());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldDeleteManagementUser() throws Exception {
        // setup
        ManagementUsers users = new ManagementUsers();
        users.getUserMap().put("a", createManagementUser("a"));
        users.getUserMap().put("b", createManagementUser("b"));

        UpdateResolverAnswer answer = new UpdateResolverAnswer(users);
        doAnswer(answer).when(writer).update(eq(id), (PiEntity) isNull(), isA(UpdateResolver.class));

        // act
        seeder.deleteUser("a");

        // assert
        ManagementUsers result = (ManagementUsers) answer.getResult();
        assertEquals(1, result.getUserMap().size());
        assertTrue(result.getUserMap().containsKey("b"));
        assertFalse(result.getUserMap().containsKey("a"));
    }

    @Test
    public void shouldGetAllManagementUsers() throws Exception {
        // setup
        ManagementUsers users = new ManagementUsers();
        users.getUserMap().put("a", createManagementUser("a"));
        users.getUserMap().put("b", createManagementUser("b"));

        when(reader.get(id)).thenReturn(users);

        // act
        ManagementUsers allUsers = seeder.getAllUsers();

        // assert
        assertNotNull(allUsers);
        assertThat(allUsers.getUserMap().size(), is(2));
    }

    // HELPER METHODS

    private ManagementUser createManagementUser(String username) {
        ManagementUser user = new ManagementUser();
        user.setUsername(username);
        user.setPassword("pass");
        user.getRoles().add(ManagementRoles.ROLE_MIS);
        return user;
    }
}
