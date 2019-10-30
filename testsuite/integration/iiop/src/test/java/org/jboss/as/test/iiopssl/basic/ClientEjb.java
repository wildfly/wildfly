/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.iiopssl.basic;

import org.jboss.as.network.NetworkUtils;

import javax.ejb.Stateless;
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

