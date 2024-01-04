/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.iiopssl.basic;

import org.jboss.as.network.NetworkUtils;

import jakarta.ejb.Stateless;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.rmi.PortableRemoteObject;
import java.rmi.RemoteException;

/**
 * @author Bartosz Spyrko-Smietanko
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
@Stateless
public class ClientEjb {

    private IIOPSslStatelessHome statelessHome;

    public String getRemoteMessage() throws RemoteException {
        IIOPSslStatelessRemote ejb = statelessHome.create();
        return ejb.hello();
    }

    public String lookup(int port) throws NamingException, RemoteException {
        final InitialContext ctx = new InitialContext();
        final String hostname = NetworkUtils.formatPossibleIpv6Address(System.getProperty("node1"));
        final Object value = ctx.lookup("corbaname:iiop:"+hostname+":" + port + "#IIOPSslStatelessBean");

        final Object narrow = PortableRemoteObject.narrow(value, IIOPSslStatelessHome.class);
        return ((IIOPSslStatelessHome)narrow).create().hello();
    }

    public String lookupSsl(int port) throws NamingException, RemoteException {
        final InitialContext ctx = new InitialContext();
        final String hostname = NetworkUtils.formatPossibleIpv6Address(System.getProperty("node1"));
        final Object value = ctx.lookup("corbaname:ssliop:1.2@"+hostname+":" + port + "#IIOPSslStatelessBean");

        final Object narrow = PortableRemoteObject.narrow(value, IIOPSslStatelessHome.class);
        return ((IIOPSslStatelessHome)narrow).create().hello();
    }
}

