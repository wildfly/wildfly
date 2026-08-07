/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.iiop.callerid;

import java.rmi.RemoteException;

import jakarta.ejb.Stateless;

@Stateless
public class ClientEjb {

    private CallerPropHome gssupHome;

    private CallerPropHome noGssupHome;

    private CallerPropHome defaultDomainHome;

    public String callGssupServer() throws RemoteException {
        CallerPropRemote ejb = gssupHome.create();
        return ejb.hello();
    }

    public String callNoGssupServer() throws RemoteException {
        CallerPropRemote ejb = noGssupHome.create();
        return ejb.hello();
    }

    public String callDefaultDomainServer() throws RemoteException {
        CallerPropRemote ejb = defaultDomainHome.create();
        return ejb.hello();
    }
}
