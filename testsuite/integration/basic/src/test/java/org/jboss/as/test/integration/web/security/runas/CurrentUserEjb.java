package org.jboss.as.test.integration.web.security.runas;

import java.security.Principal;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

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
