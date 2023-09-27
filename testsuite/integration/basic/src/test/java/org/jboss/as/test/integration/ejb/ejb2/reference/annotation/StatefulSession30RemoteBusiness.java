/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.ejb2.reference.annotation;

import java.rmi.RemoteException;

/**
 * StatefulSession30RemoteBusiness
 *
 * @author <a href="mailto:andrew.rubinger@jboss.org">ALR</a>
 */
public interface StatefulSession30RemoteBusiness {
    void setValue(String value) throws RemoteException;

    String getValue() throws RemoteException;

    String accessLocalStateless() throws RemoteException;

    String accessLocalHome() throws RemoteException;
}
