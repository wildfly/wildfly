/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security;

import java.rmi.RemoteException;
import jakarta.ejb.EJBObject;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public interface HelloRemote extends EJBObject {
    String sayHello(String name) throws RemoteException;
}
