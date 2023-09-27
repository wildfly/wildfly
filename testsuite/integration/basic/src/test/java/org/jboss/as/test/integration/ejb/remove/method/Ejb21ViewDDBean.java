/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.remove.method;

import java.rmi.RemoteException;
import jakarta.ejb.EJBException;
import jakarta.ejb.SessionBean;
import jakarta.ejb.SessionContext;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author Jaikiran Pai
 */
public class Ejb21ViewDDBean implements SessionBean {

    @Override
    public void setSessionContext(SessionContext sessionContext) throws EJBException, RemoteException {

    }

    public void ejbCreate() {
    }

    @Override
    public void ejbRemove() throws EJBException, RemoteException {
        try {
            final Context ctx = new InitialContext();
            final RemoveMethodInvocationTracker removeMethodInvocationTracker = (RemoveMethodInvocationTracker) ctx.lookup("java:app/remove-method-test/" + RemoveMethodInvocationTrackerBean.class.getSimpleName() + "!" + RemoveMethodInvocationTracker.class.getName());
            removeMethodInvocationTracker.ejbRemoveCallbackInvoked();
        } catch (NamingException ne) {
            throw new RuntimeException(ne);
        }

    }

    @Override
    public void ejbActivate() throws EJBException, RemoteException {
    }

    @Override
    public void ejbPassivate() throws EJBException, RemoteException {
    }

    public String test() throws RemoteException {
        return this.getClass().getName();
    }

}
