/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.servlet;

import java.rmi.RemoteException;
import jakarta.ejb.EJBHome;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
public interface Session30Home extends EJBHome {
    Session30Remote create() throws jakarta.ejb.CreateException,  RemoteException;
}
