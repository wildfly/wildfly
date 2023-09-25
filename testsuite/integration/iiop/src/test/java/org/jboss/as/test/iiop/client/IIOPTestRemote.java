/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiop.client;

import java.rmi.RemoteException;

import jakarta.ejb.EJBObject;

public interface IIOPTestRemote extends EJBObject {
    String callMandatory() throws RemoteException;
    String callNever() throws RemoteException;
    String callRollbackOnly() throws RemoteException;
    int transactionStatus() throws RemoteException;
}
