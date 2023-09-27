/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb2.stateful.failover.bean.shared;

import jakarta.ejb.EJBHome;

/**
 * @author Ondrej Chaloupka
 */
public interface CounterRemoteHome extends EJBHome {
    CounterRemote create() throws java.rmi.RemoteException, jakarta.ejb.CreateException;

}
