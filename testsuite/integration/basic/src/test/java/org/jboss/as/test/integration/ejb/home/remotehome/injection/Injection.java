/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.home.remotehome.injection;

import java.rmi.RemoteException;

import jakarta.ejb.EJBObject;

/**
 * @author Stuart Douglas
 */
public interface Injection extends EJBObject {

    String message() throws RemoteException;

}
