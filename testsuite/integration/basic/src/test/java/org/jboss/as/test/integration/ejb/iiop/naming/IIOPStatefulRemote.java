/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.iiop.naming;

import java.rmi.RemoteException;

import jakarta.ejb.EJBObject;

/**
 * @author Stuart Douglas
 */
public interface IIOPStatefulRemote extends EJBObject {

    int increment() throws RemoteException;
}
