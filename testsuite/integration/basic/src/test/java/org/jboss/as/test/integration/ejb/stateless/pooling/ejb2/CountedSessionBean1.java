/*
 * JBoss, Home of Professional Open Source
 * Copyright (c) 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.ejb.stateless.pooling.ejb2;

import java.rmi.RemoteException;

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
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
