/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb2.stateful.passivation;

import java.rmi.RemoteException;
import jakarta.ejb.CreateException;
import jakarta.ejb.EJBHome;

/**
 * @author Ondrej Chaloupka
 */
public interface StatefulRemoteHome extends EJBHome {
    StatefulRemote create() throws RemoteException, CreateException;

}
