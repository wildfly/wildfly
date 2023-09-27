/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.web.sso.interfaces;

import java.rmi.RemoteException;

import jakarta.ejb.CreateException;
import jakarta.ejb.EJBHome;

public interface StatelessSessionHome extends EJBHome {
    StatelessSession create() throws RemoteException, CreateException;
}
