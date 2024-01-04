/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateless.pooling.ejb2;

import java.rmi.RemoteException;

import jakarta.ejb.EJBException;
import jakarta.ejb.SessionBean;
import jakarta.ejb.SessionContext;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:dimitris@jboss.org">Dimitris Andreadis</a>
 */
public class CountedSessionBean1 implements SessionBean {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(CountedSessionBean1.class);
    protected SessionContext ctx;

    public CountedSessionBean1() {
        log.trace("CTOR1");
    }

    // Business Methods ----------------------------------------------
    public void doSomething(long delay) {
        log.trace("doSomething(" + delay + ")");
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void doSomethingSync(long delay) {
        try {
            doSomething(delay);
        } finally {
            EjbRemoveUnitTestCase.CDL.countDown();
        }
    }

    // Container callbacks -------------------------------------------
    public void setSessionContext(SessionContext ctx) throws EJBException, RemoteException {
        this.ctx = ctx;
        log.trace("setSessionContext");
    }

    public void ejbCreate() throws RemoteException  {
        log.trace("ejbCreate[1]: " + CounterSingleton.createCounter1.incrementAndGet());
    }

    public void ejbRemove() {
        try {
            log.trace("ejbRemove[1]: " + CounterSingleton.removeCounter1.incrementAndGet());
        } catch (Exception e) {
            log.error("Ignored exception", e);
        }
    }

    public void ejbActivate() {
        log.trace("ejbActivate");
    }

    public void ejbPassivate() {
        log.trace("ejbPassivate");
    }
}
