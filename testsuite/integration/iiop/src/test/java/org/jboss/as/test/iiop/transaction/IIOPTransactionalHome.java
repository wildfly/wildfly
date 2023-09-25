/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiop.transaction;

import java.rmi.RemoteException;

import jakarta.ejb.EJBHome;

/**
 * @author Stuart Douglas
 */
public interface IIOPTransactionalHome extends EJBHome {

    IIOPTransactionalRemote create() throws RemoteException;

}
