/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiop.callerid;

import java.rmi.RemoteException;

import jakarta.ejb.EJBHome;

public interface CallerPropHome extends EJBHome {

    CallerPropRemote create() throws RemoteException;
}
