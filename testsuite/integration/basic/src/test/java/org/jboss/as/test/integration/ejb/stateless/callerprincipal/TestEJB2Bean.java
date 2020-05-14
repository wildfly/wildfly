package org.jboss.as.test.integration.ejb.stateless.callerprincipal;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import java.rmi.RemoteException;
import java.util.logging.Logger;

public class TestEJB2Bean implements SessionBean, TestEJB2Int {

    private static final Logger log = Logger.getLogger(TestEJB2Bean.class.getName());

    private static final String USER = "user1";

    public boolean isCallerInRole(String role) throws Exception {
        String caller = ctx.getCallerPrincipal().getName();
        if (!USER.equals(caller)) {
            throw new Exception("Caller name is not " + USER + ", but " + caller);
        }

        return ctx.isCallerInRole(role);
    }

    private SessionContext ctx;

    public void setSessionContext(SessionContext pCtx) {
        ctx = pCtx;
    }

    public void ejbCreate() throws RemoteException, CreateException {
    }

    public void ejbRemove() throws RemoteException {
    }

    public void ejbActivate() throws RemoteException {
    }

    public void ejbPassivate() throws RemoteException {
    }
}
