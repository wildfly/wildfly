package org.jboss.as.test.iiop.security;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.RemoteHome;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * @author Stuart Douglas
 */
@RemoteHome(IIOPSecurityStatelessHome.class)
@Stateless
@SecurityDomain("other")
public class IIOPSecurityStatelessBean {

    @RolesAllowed("Role1")
    public String role1() {
        return "role1";
    }

    @RolesAllowed("Role2")
    public String role2() {
        return "role2";
    }

    public void ejbCreate() {

    }

    public void ejbActivate() {

    }

    public void ejbPassivate() {

    }

    public void ejbRemove() {

    }

    public void setSessionContext(SessionContext context) {

    }
}
