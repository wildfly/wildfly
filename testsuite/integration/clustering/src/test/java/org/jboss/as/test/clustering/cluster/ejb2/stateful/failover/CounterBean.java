/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb2.stateful.failover;

import jakarta.ejb.EJBException;
import jakarta.ejb.RemoteHome;
import jakarta.ejb.SessionBean;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateful;

import org.jboss.as.test.clustering.cluster.ejb2.stateful.failover.bean.shared.CounterBaseBean;
import org.jboss.as.test.clustering.cluster.ejb2.stateful.failover.bean.shared.CounterRemoteHome;
import org.jboss.as.test.clustering.cluster.ejb2.stateful.failover.bean.singleton.CounterSingleton;
import org.jboss.logging.Logger;

/**
 * @author Ondrej Chaloupka
 */
@Stateful
@RemoteHome(CounterRemoteHome.class)
public class CounterBean extends CounterBaseBean implements SessionBean {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(CounterBean.class);

    @Override
    public void ejbRemove() throws EJBException {
        log.trace("ejbRemove called...");
        CounterSingleton.destroyCounter.incrementAndGet();
    }

    @Override
    public void ejbActivate() throws EJBException {

    }

    @Override
    public void ejbPassivate() throws EJBException {

    }

    @Override
    public void setSessionContext(SessionContext arg0) throws EJBException {

    }
}
