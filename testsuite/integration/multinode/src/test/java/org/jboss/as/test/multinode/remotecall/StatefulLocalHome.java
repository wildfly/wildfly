/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.multinode.remotecall;

import jakarta.ejb.EJBHome;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
public interface StatefulLocalHome extends EJBHome {
    StatefulLocal create() throws java.rmi.RemoteException, jakarta.ejb.CreateException;
}
