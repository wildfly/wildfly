package org.jboss.as.test.clustering.cluster.ejb2.remote.bean;

import javax.ejb.EJBException;
import javax.ejb.SessionContext;
import java.rmi.RemoteException;
import java.util.Date;

public abstract class HeartbeatBeanBase {

    public Result<Date> pulse() {
        Date now = new Date();
        return new Result<>(now);
    }

    public void ejbCreate() throws EJBException, RemoteException {
        // creating ejb2 bean
    }

    public void setSessionContext(SessionContext sessionContext) throws EJBException, RemoteException {

    }

    public void ejbRemove() throws EJBException, RemoteException {

    }

    public void ejbActivate() throws EJBException, RemoteException {

    }

    public void ejbPassivate() throws EJBException, RemoteException {

    }
}
