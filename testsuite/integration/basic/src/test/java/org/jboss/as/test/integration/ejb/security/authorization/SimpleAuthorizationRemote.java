package org.jboss.as.test.integration.ejb.security.authorization;

import javax.ejb.Remote;

/**
 * @author <a href="mailto:pskopek@redhat.com">Peter Skopek</a>
 */
@Remote
public interface SimpleAuthorizationRemote {

    String defaultAccess(String message);

    String roleBasedAccessOne(String message);

    String roleBasedAccessMore(String message);

    String permitAll(String message);

    String denyAll(String message);

    String starRoleAllowed(String message);

}
