/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.sessioncontext;

import java.rmi.RemoteException;
import jakarta.annotation.Resource;
import jakarta.ejb.EJBException;
import jakarta.ejb.SessionBean;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateful;

/**
 * User: Jaikiran Pai
 */
@Stateful
public class SFSBImplementingSessionBean implements SessionBean {
    private static final long serialVersionUID = 1L;

    private SessionContext sessionContext;

    private SessionContext injectedSessionContext;

    @Override
    public void setSessionContext(SessionContext ctx) throws EJBException, RemoteException {
        this.sessionContext = ctx;
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

    @Resource
    public void setInjectedSessionContext(SessionContext ctx) {
        this.injectedSessionContext = ctx;
    }

    public boolean wasSetSessionContextMethodInvoked() {
        return this.sessionContext != null;
    }

    public boolean wasSessionContextInjected() {
        return this.injectedSessionContext != null;
    }

}
