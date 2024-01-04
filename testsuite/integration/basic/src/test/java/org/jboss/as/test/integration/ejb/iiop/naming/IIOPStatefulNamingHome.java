/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.iiop.naming;

import java.rmi.RemoteException;

import jakarta.ejb.EJBHome;

/**
 * @author Stuart Douglas
 */
public interface IIOPStatefulNamingHome extends EJBHome {

    IIOPStatefulRemote create(int start) throws RemoteException;

}
