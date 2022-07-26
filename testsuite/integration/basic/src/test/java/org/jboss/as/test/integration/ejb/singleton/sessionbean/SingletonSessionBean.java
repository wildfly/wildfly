package org.jboss.as.test.integration.ejb.singleton.sessionbean;

import jakarta.ejb.EJBException;
import jakarta.ejb.SessionBean;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.rmi.RemoteException;

@Singleton
@Startup
public class SingletonSessionBean implements SessionBean {
    @Override
    public void setSessionContext(SessionContext ctx) throws EJBException, RemoteException {
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
}
