/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiop.denylist;

import java.rmi.RemoteException;
import jakarta.ejb.EJBHome;

/**
 * Home interface for IIOP deny-list test bean.
 */
public interface IIOPDenyListHome extends EJBHome {
    IIOPDenyListRemote create() throws RemoteException;
}
