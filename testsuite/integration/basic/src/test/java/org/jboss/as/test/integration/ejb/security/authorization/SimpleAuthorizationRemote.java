package org.jboss.as.test.integration.ejb.security.authorization;

import javax.ejb.Remote;

/**
 * @author <a href="mailto:pskopek@redhat.com">Peter Skopek</a>
 */
@Remote
public interface SimpleAuthorizationRemote {

    public String defaultAccess(String message);

    public String roleBasedAccessOne(String message);

    public String roleBasedAccessMore(String message);

    public String permitAll(String message);

    public String denyAll(String message);

}
