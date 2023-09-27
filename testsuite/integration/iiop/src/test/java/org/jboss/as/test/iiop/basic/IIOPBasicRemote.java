/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiop.basic;

import java.rmi.RemoteException;

import jakarta.ejb.EJBObject;

/**
 * @author Stuart Douglas
 */
public interface IIOPBasicRemote extends EJBObject {

    String hello() throws RemoteException;

    HandleWrapper wrappedHandle() throws RemoteException;
}
