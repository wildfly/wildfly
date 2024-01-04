/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.home.remotehome.injection;

import java.rmi.RemoteException;

import jakarta.ejb.EJBHome;

/**
 * @author Stuart Douglas
 */
public interface InjectionHome extends EJBHome{

    Injection create() throws RemoteException;
}
