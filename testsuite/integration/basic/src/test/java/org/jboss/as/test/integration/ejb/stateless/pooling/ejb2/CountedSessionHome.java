/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateless.pooling.ejb2;

import java.rmi.RemoteException;

import jakarta.ejb.CreateException;
import jakarta.ejb.EJBHome;

/**
 * @author <a href="mailto:dimitris@jboss.org">Dimitris Andreadis</a>
 */
public interface CountedSessionHome extends EJBHome {
    CountedSession create() throws RemoteException, CreateException;
}
