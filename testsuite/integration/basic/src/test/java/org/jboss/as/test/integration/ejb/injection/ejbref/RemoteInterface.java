/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.injection.ejbref;

import java.rmi.RemoteException;
import jakarta.ejb.EJBObject;

/**
 * @author Stuart Douglas
 */
public interface RemoteInterface extends EJBObject {

    String hello() throws RemoteException;
}
