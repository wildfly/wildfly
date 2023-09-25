/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.ejb2.reference.eararchive;

import jakarta.ejb.EJBHome;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
public interface Test3Home extends EJBHome {

    Test3 create() throws java.rmi.RemoteException, jakarta.ejb.CreateException;
}
