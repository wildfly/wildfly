/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiop.transaction.timeout;

import jakarta.ejb.EJBHome;
import java.rmi.RemoteException;

public interface TestBeanHome extends EJBHome {
    TestBeanRemote create() throws RemoteException;
}
