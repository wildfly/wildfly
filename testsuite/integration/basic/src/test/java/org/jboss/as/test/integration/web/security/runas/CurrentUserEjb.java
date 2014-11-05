package org.jboss.as.test.integration.web.security.runas;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import java.security.Principal;

/**
 * @author Stuart Douglas
 */
@Stateless
@PermitAll
public class CurrentUserEjb {

    @Resource
    private SessionContext sessionContext;

    public String getCurrentUser() {
        Principal callerPrincipal = sessionContext.getCallerPrincipal();
        if (callerPrincipal == null) {
            return null;
        }
        return callerPrincipal.getName();
    }

}
