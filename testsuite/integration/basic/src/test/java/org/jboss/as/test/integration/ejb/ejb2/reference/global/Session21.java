/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.ejb2.reference.global;

import java.rmi.RemoteException;

/**
 * @author $Author: wolfc
 */
public interface Session21 extends jakarta.ejb.EJBObject {
    String access() throws RemoteException;

    String access30() throws RemoteException;

    String globalAccess30() throws RemoteException;
}
