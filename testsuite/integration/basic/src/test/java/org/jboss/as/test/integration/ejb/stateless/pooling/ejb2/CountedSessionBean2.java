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
