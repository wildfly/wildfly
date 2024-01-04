/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.home.remotehome;

import java.rmi.RemoteException;

import jakarta.ejb.EJBHome;

/**
 *
 * @author Stuart Douglas
 */
public interface SimpleStatefulHome extends EJBHome {

    SimpleInterface createSimple(String message) throws RemoteException;

    SimpleInterface createComplex(String first, String second) throws RemoteException;

}
