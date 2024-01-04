/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.home.remotehome;

import java.rmi.RemoteException;

import jakarta.ejb.EJBObject;

/**
 * @author Stuart Douglas
 */
public interface SimpleInterface extends EJBObject {

    String sayHello() throws RemoteException;

    String otherMethod() throws RemoteException;
}
