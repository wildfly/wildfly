package org.jboss.as.test.integration.ejb.singleton.sessionbean;

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.Startup;
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
