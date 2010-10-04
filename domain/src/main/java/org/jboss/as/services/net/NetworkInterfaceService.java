/*
* JBoss, Home of Professional Open Source
* Copyright 2010, Red Hat Inc., and individual contributors as indicated
* by the @authors tag. See the copyright.txt in the distribution for a
* full listing of individual contributors.
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
package org.jboss.as.services.net;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;

import org.jboss.as.model.socket.AbstractInterfaceElement;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service resolving the {@code NetworkInterfaceBinding} based on the configured
 * interfaces in the domain model.
 *
 * @author Emanuel Muckenhuber
 */
public class NetworkInterfaceService implements Service<NetworkInterfaceBinding> {

    /** The service base name. */
    public static final ServiceName JBOSS_NETWORK_INTERFACE = ServiceName.JBOSS.append("network");

    private static final boolean preferIPv4Stack = Boolean.getBoolean("java.net.preferIPv4Stack");

    private static final String IPV4_ANYLOCAL = "0.0.0.0";
    private static final String IPV6_ANYLOCAL = "::";

    /** The interface binding. */
    private NetworkInterfaceBinding interfaceBinding;

    /** The network interface element. */
    private final AbstractInterfaceElement<?> interfaceElement;

    public NetworkInterfaceService(final AbstractInterfaceElement<?> element) {
        this.interfaceElement = element;
    }

    public synchronized void start(StartContext arg0) throws StartException {
        try {
            if (this.interfaceElement.isAnyLocalV4Address()) {
                this.interfaceBinding = getNetworkInterfaceBinding(IPV4_ANYLOCAL);
            }
            else if (this.interfaceElement.isAnyLocalV6Address()) {
                this.interfaceBinding = getNetworkInterfaceBinding(IPV6_ANYLOCAL);
            }
            else if (this.interfaceElement.isAnyLocalAddress()) {
                this.interfaceBinding = getNetworkInterfaceBinding(preferIPv4Stack ? IPV4_ANYLOCAL : IPV6_ANYLOCAL);
            }
            else {
                this.interfaceBinding = resolveInterface(interfaceElement);
            }
        } catch(Exception e) {
            throw new StartException(e);
        }
        if(this.interfaceBinding == null) {
            throw new StartException("failed to resolve interface for " + interfaceElement.getName());
        }
    }

    public synchronized void stop(StopContext arg0) {
        this.interfaceBinding = null;
    }

    public synchronized NetworkInterfaceBinding getValue() throws IllegalStateException {
        final NetworkInterfaceBinding binding = this.interfaceBinding;
        if(binding == null) {
            throw new IllegalStateException();
        }
        return binding;
    }

    static NetworkInterfaceBinding resolveInterface(final AbstractInterfaceElement<?> element) throws SocketException {
        final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            final NetworkInterface networkInterface = networkInterfaces.nextElement();
            final Enumeration<InetAddress> interfaceAddresses = networkInterface.getInetAddresses();
            while (interfaceAddresses.hasMoreElements()) {
                final InetAddress address = interfaceAddresses.nextElement();
                if (element.getInterfaceCriteria().isAcceptable(networkInterface, address)) {
                    final InetAddress resolved = getInterfaceAddress(networkInterface);
                    if (resolved != null) {
                        return new NetworkInterfaceBinding(Collections.singleton(networkInterface), resolved);
                    }
                }
            }
        }
        return null;
    }

    static InetAddress getInterfaceAddress(final NetworkInterface networkInterface) {
        final Enumeration<InetAddress> interfaceAddresses = networkInterface.getInetAddresses();
        while(interfaceAddresses.hasMoreElements()) {
            final InetAddress address = interfaceAddresses.nextElement();
            // prefer IPv4 stack
            if(preferIPv4Stack && address instanceof Inet4Address) {
                return address;
            } else if (! preferIPv4Stack) {
                return address;
            }
        }
        return null;
    }

    static NetworkInterfaceBinding getNetworkInterfaceBinding(final String addr) throws UnknownHostException, SocketException {
        final InetAddress address = InetAddress.getByName(addr);
        final Collection<NetworkInterface> interfaces = new ArrayList<NetworkInterface>();
        final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            interfaces.add(networkInterfaces.nextElement());
        }
        return new NetworkInterfaceBinding(interfaces, address);
    }

}

