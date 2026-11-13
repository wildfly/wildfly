/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiop.callerid;

import java.rmi.RemoteException;

import jakarta.ejb.EJBObject;

public interface CallerPropRemote extends EJBObject {

    String hello() throws RemoteException;
}
