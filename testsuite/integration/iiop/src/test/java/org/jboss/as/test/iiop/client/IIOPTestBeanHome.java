/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiop.client;

import java.rmi.RemoteException;
import jakarta.ejb.EJBHome;

public interface IIOPTestBeanHome extends EJBHome {

    IIOPTestRemote create() throws RemoteException;
}
