/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateless.pooling.ejb2;

import java.rmi.RemoteException;
import jakarta.ejb.EJBObject;

/**
 * @author <a href="mailto:dimitris@jboss.org">Dimitris Andreadis</a>
 */
public interface CountedSession extends EJBObject {
    void doSomething(long delay) throws RemoteException;
    void doSomethingSync(long delay) throws RemoteException;
}
