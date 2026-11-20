/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiopssl.callerid;

import java.rmi.RemoteException;

import jakarta.ejb.Stateless;

@Stateless
public class ClientEjb {

    private CallerPropHome successHome;

    private CallerPropHome failureHome;

    private CallerPropHome bothAuthHome;

    public String callServer() throws RemoteException {
        CallerPropRemote ejb = successHome.create();
        return ejb.hello();
    }

    public String callDefaultDomainServer() throws RemoteException {
        CallerPropRemote ejb = failureHome.create();
        return ejb.hello();
    }

    public String callGssupAndTlsServer() throws RemoteException {
        CallerPropRemote ejb = bothAuthHome.create();
        return ejb.hello();
    }
}
