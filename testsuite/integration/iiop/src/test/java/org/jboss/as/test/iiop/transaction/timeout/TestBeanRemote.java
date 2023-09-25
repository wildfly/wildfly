/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiop.transaction.timeout;

import java.rmi.RemoteException;
import jakarta.ejb.EJBObject;

public interface TestBeanRemote extends EJBObject {
    void testTransaction() throws RemoteException;
    void testTimeout() throws RemoteException;
    void touch() throws RemoteException;
}

