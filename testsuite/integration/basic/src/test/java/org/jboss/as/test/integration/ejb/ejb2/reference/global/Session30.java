/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.ejb2.reference.global;

import java.rmi.RemoteException;

import jakarta.ejb.EJBObject;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
public interface Session30 extends EJBObject {
    String access() throws RemoteException;

    String access21() throws RemoteException;

    String globalAccess21() throws RemoteException;

    String accessLocalStateful() throws RemoteException;

    String accessLocalStateful(String value) throws RemoteException;

    String accessLocalStateful(String value, Integer suffix) throws RemoteException;
}
