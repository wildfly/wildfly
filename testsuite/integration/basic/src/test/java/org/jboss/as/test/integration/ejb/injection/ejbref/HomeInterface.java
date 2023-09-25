/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.injection.ejbref;

import java.rmi.RemoteException;
import jakarta.ejb.EJBHome;

/**
 * @author Stuart Douglas
 */
public interface HomeInterface extends EJBHome {
    RemoteInterface create() throws RemoteException;
}
