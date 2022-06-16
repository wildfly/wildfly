package org.jboss.as.test.integration.ejb.security;

import java.rmi.RemoteException;
import jakarta.ejb.EJBException;
import jakarta.ejb.RemoteHome;
import jakarta.ejb.SessionBean;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;

import org.jboss.ejb3.annotation.SecurityDomain;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Stateless
@SecurityDomain("other")
@RemoteHome(HelloHome.class)
public class HelloBean implements SessionBean {
    private SessionContext ctx;

    @Override
    public void setSessionContext(SessionContext ctx) throws EJBException, RemoteException {
        this.ctx = ctx;
    }

    @Override
    public void ejbRemove() throws EJBException, RemoteException {
    }

    @Override
    public void ejbActivate() throws EJBException, RemoteException {
    }

    @Override
    public void ejbPassivate() throws EJBException, RemoteException {
    }

    public String sayHello(final String name) {
        return "Hello " + name;
    }
}
