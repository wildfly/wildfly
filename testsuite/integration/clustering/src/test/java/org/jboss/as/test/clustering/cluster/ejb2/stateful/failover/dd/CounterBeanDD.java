/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb2.stateful.failover.dd;

import java.rmi.RemoteException;

import jakarta.ejb.EJBException;
import jakarta.ejb.SessionBean;
import jakarta.ejb.SessionContext;

import org.jboss.as.test.clustering.cluster.ejb2.stateful.failover.bean.shared.CounterBaseBean;
import org.jboss.as.test.clustering.cluster.ejb2.stateful.failover.bean.singleton.CounterSingleton;
import org.jboss.logging.Logger;

/**
 * @author Ondrej Chaloupka
 */
public class CounterBeanDD extends CounterBaseBean implements SessionBean {
    private static final Logger log = Logger.getLogger(CounterBeanDD.class);
    private static final long serialVersionUID = 1L;

    @Override
    public void ejbRemove() throws EJBException, RemoteException {
        log.trace("ejbRemove() was called..");
        CounterSingleton.destroyCounter.incrementAndGet();
    }

    @Override
    public void ejbActivate() throws EJBException, RemoteException {

    }

    @Override
    public void ejbPassivate() throws EJBException, RemoteException {

    }

    @Override
    public void setSessionContext(SessionContext arg0) throws EJBException, RemoteException {

    }
}
