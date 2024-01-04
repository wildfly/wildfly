/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.security.runas.ejb2mdb;

import java.rmi.RemoteException;
import jakarta.ejb.EJBObject;

/**
 * @author Ondrej Chaloupka
 */
public interface GoodBye extends EJBObject {
    String sayGoodBye() throws RemoteException;
}
