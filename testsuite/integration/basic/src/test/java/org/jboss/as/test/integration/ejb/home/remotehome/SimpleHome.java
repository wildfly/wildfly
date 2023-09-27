/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.home.remotehome;

import java.rmi.RemoteException;

import jakarta.ejb.EJBHome;

/**
 * Simple local home interface
 *
 * @author Stuart Douglas
 */
public interface SimpleHome extends EJBHome {

    SimpleInterface createSimple() throws RemoteException;

}
