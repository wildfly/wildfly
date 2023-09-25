/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.remove.method;

import java.rmi.RemoteException;
import jakarta.ejb.EJBHome;

/**
 * @author carlo
 */
public interface Ejb21ViewHome extends EJBHome {
    Ejb21View create() throws RemoteException;
}
