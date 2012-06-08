/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website;

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.bt.pi.app.common.entities.ManagementRoles;
import com.bt.pi.app.common.entities.ManagementUser;
import com.bt.pi.app.common.entities.ManagementUsers;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.core.dht.cache.BlockingDhtCache;

public class PiOpsUserDetailsService implements UserDetailsService {
	private static final Log LOG = LogFactory.getLog(PiOpsUserDetailsService.class);

	private BlockingDhtCache blockingDhtCache;
	private PiIdBuilder piIdBuilder;

	public PiOpsUserDetailsService() {
		blockingDhtCache = null;
		piIdBuilder = null;
	}

	@Resource(name = "userBlockingCache")
	public void setUserCache(BlockingDhtCache aBlockingDhtCache) {
		this.blockingDhtCache = aBlockingDhtCache;
	}

	@Resource
	public void setPiIdBuilder(PiIdBuilder builder) {
		this.piIdBuilder = builder;
	}

	@Override
	public UserDetails loadUserByUsername(String username) {
		try {
			ManagementUsers users = (ManagementUsers) blockingDhtCache.get(piIdBuilder.getPId(new ManagementUsers()));
			if (users == null || !users.getUserMap().containsKey(username)) {
				LOG.info("Unable to find management user " + username);
				throw new UsernameNotFoundException(username + " is not a valid user");
			}
			ManagementUser mu = users.getUserMap().get(username);
			Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
			for (ManagementRoles role : mu.getRoles()) {
				authorities.add(new GrantedAuthorityImpl(role.name()));
			}
			User user = new User(mu.getUsername(), mu.getPassword(), true, true, true, true, authorities);
			StringBuilder sb = new StringBuilder("loaded management user ").append(username).append(" with authorities ");
			for (GrantedAuthority grantedAuthority : authorities) {
				sb.append(grantedAuthority.getAuthority()).append(" ");
			}
			LOG.debug(sb.toString());
			return user;
		} catch (UsernameNotFoundException e) {
			throw e;
		} catch (Throwable t) {
			LOG.error(t.getMessage(), t);
			throw new UsernameNotFoundException("Unexpected error when checking user credentials");
		}
	}
}
