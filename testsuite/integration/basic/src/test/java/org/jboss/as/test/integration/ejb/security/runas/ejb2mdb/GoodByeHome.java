/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.runas.ejb2mdb;

import java.rmi.RemoteException;
import jakarta.ejb.CreateException;
import jakarta.ejb.EJBHome;

/**
 * @author Ondrej Chaloupka
 */
public interface GoodByeHome extends EJBHome {
    GoodBye create() throws RemoteException, CreateException;
}
