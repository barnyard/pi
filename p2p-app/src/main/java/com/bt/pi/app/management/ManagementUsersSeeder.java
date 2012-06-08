/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.app.management;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.ManagementRoles;
import com.bt.pi.app.common.entities.ManagementUser;
import com.bt.pi.app.common.entities.ManagementUsers;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.id.PId;

@Component
public class ManagementUsersSeeder extends SeederBase {
    private static final Log LOG = LogFactory.getLog(ManagementUsersSeeder.class);

    public ManagementUsersSeeder() {
        super();
    }

    public void addUser(final String userName, String password, String... roles) {
        LOG.debug(String.format("addUser(%s, %.3s, %s)", userName, password, roles));

        final ManagementUser user = new ManagementUser();
        user.setUsername(userName);
        user.setPassword(password);
        for (String role : roles) {
            if (!StringUtils.isEmpty(role))
                user.getRoles().add(ManagementRoles.valueOf(role));
        }

        final ManagementUsers users = new ManagementUsers();
        final PId managementUsersId = getPiIdBuilder().getPId(users);
        users.getUserMap().put(userName, user);

        getDhtClientFactory().createBlockingWriter().update(managementUsersId, null, new UpdateResolver<ManagementUsers>() {

            @Override
            public ManagementUsers update(ManagementUsers existingEntity, ManagementUsers requestedEntity) {

                if (null == existingEntity)
                    return users;

                existingEntity.getUserMap().put(userName, user);
                return existingEntity;
            }
        });
    }

    public ManagementUsers getAllUsers() {
        LOG.debug(String.format("getAllUsers()"));

        final ManagementUsers users = new ManagementUsers();
        final PId managementUsersId = getPiIdBuilder().getPId(users);

        ManagementUsers managementUsers = (ManagementUsers) getDhtClientFactory().createBlockingReader().get(managementUsersId);

        return managementUsers;
    }

    public void deleteUser(final String username) {
        LOG.debug(String.format("deleteUser(%s)", username));

        final ManagementUsers users = new ManagementUsers();
        final PId managementUsersId = getPiIdBuilder().getPId(users);

        getDhtClientFactory().createBlockingWriter().update(managementUsersId, null, new UpdateResolver<ManagementUsers>() {
            @Override
            public ManagementUsers update(ManagementUsers existingEntity, ManagementUsers requestedEntity) {
                if (existingEntity == null)
                    return null;

                existingEntity.getUserMap().remove(username);
                return existingEntity;
            }
        });
    }

    public ManagementUser getUser(String username) {
        LOG.debug(String.format("getUser(%s)", username));

        final ManagementUsers users = new ManagementUsers();
        final PId managementUsersId = getPiIdBuilder().getPId(users);

        ManagementUsers managementUsers = (ManagementUsers) getDhtClientFactory().createBlockingReader().get(managementUsersId);

        return managementUsers.getUserMap().get(username);
    }
}
