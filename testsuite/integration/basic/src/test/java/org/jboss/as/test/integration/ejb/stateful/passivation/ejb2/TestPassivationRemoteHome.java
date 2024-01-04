/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.stateful.passivation.ejb2;

import jakarta.ejb.EJBHome;

/**
 * @author Ondrej Chaloupka
 */
public interface TestPassivationRemoteHome extends EJBHome {
    TestPassivationRemote create() throws java.rmi.RemoteException, jakarta.ejb.CreateException;
}
