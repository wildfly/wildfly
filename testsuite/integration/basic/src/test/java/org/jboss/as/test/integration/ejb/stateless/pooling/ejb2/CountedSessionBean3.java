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
public class CountedSessionBean3 extends CountedSessionBean1 {
    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(CountedSessionBean3.class);

    public CountedSessionBean3() {
        log.trace("CTOR3");
    }

    public void ejbCreate() throws RemoteException {
        log.trace("ejbCreate[3]: " + CounterSingleton.createCounter3.incrementAndGet());
    }

    public void ejbRemove() {
        try {
            log.trace("ejbRemove[3]: " + CounterSingleton.removeCounter3.incrementAndGet());
        } catch (Exception e) {
            log.error("Ignored exception", e);
        }
    }
}
