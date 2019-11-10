package org.jboss.as.test.integration.ejb.stateless.callerprincipal;

import javax.annotation.Resource;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import java.util.logging.Logger;

@Stateless
@Remote(TestEJB3Remote.class)
@RolesAllowed({ "Users" })
public class TestEJB3 implements TestEJB3Remote {

    private static final Logger log = Logger.getLogger(TestEJB3.class.getName());

    private static final String USER = "user1";

    @Resource
    private SessionContext ctx;

    public boolean isCallerInRole(String role) throws Exception {
        String caller = ctx.getCallerPrincipal().getName();
        if (!USER.equals(caller)) {
            throw new Exception("Caller name is not " + USER + ", but " + caller);
        }

        return ctx.isCallerInRole(role);
    }

}