/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateless.pooling.ejb2;

import java.rmi.RemoteException;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:dimitris@jboss.org">Dimitris Andreadis</a>
 */
public class CountedSessionBean2 extends CountedSessionBean1 {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(CountedSessionBean2.class);

    public CountedSessionBean2() {
        log.trace("CTOR2");
    }

    public void ejbCreate() throws RemoteException {
        log.trace("ejbCreate[2]: " + CounterSingleton.createCounter2.incrementAndGet());
    }

    public void ejbRemove() {
        try {
            log.trace("ejbRemove[2]: " + CounterSingleton.removeCounter2.incrementAndGet());
        } catch (Exception e) {
            log.error("Ignored exception", e);
        }
    }
}
