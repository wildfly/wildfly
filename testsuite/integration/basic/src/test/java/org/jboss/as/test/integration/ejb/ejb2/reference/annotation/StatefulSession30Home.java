/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.ejb2.reference.annotation;

import jakarta.ejb.EJBHome;

/**
 * @author <a href="mailto:bdecoste@jboss.com">William DeCoste</a>
 */
public interface StatefulSession30Home extends EJBHome {

    StatefulSession30 create() throws java.rmi.RemoteException, jakarta.ejb.CreateException;

    StatefulSession30 create(String value) throws java.rmi.RemoteException, jakarta.ejb.CreateException;

    StatefulSession30 create(String value, Integer suffix) throws java.rmi.RemoteException, jakarta.ejb.CreateException;
}
