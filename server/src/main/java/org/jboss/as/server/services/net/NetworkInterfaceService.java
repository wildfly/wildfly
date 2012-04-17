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
package org.jboss.as.server.services.net;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.interfaces.InterfaceCriteria;
import org.jboss.as.controller.interfaces.OverallInterfaceCriteria;
import org.jboss.as.controller.interfaces.ParsedInterfaceCriteria;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.server.ServerLogger;
import org.jboss.as.server.ServerMessages;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service resolving the {@code NetworkInterfaceBinding} based on the configured interfaces in the domain model.
 *
 * @author Emanuel Muckenhuber Scott stark (sstark@redhat.com) (C) 2011 Red Hat Inc.
 */
public class NetworkInterfaceService implements Service<NetworkInterfaceBinding> {

    private static ServerLogger log = ServerLogger.NETWORK_LOGGER;

    /** The service base name. */
    public static final ServiceName JBOSS_NETWORK_INTERFACE = ServiceName.JBOSS.append("network");

    private static final String IPV4_ANYLOCAL = "0.0.0.0";
    private static final String IPV6_ANYLOCAL = "::";

    /** The interface binding. */
    private NetworkInterfaceBinding interfaceBinding;

    private final String name;
    private final boolean anyLocalV4;
    private final boolean anyLocalV6;
    private final boolean anyLocal;
    private final OverallInterfaceCriteria criteria;

    public static Service<NetworkInterfaceBinding> create(String name, ParsedInterfaceCriteria criteria) {
        return new NetworkInterfaceService(name, criteria.isAnyLocalV4(), criteria.isAnyLocalV6(), criteria.isAnyLocal(),
                                           criteria.getCriteria());
    }

    public NetworkInterfaceService(final String name, final boolean anyLocalV4, final boolean anyLocalV6,
            final boolean anyLocal, final Set<InterfaceCriteria> criteria) {
        this.name = name;
        this.anyLocalV4 = anyLocalV4;
        this.anyLocalV6 = anyLocalV6;
        this.anyLocal = anyLocal;
        this.criteria = new OverallInterfaceCriteria(name, criteria);
    }

    public synchronized void start(StartContext arg0) throws StartException {
        log.debug("Starting NetworkInterfaceService\n");
        try {
            this.interfaceBinding = createBinding(anyLocalV4, anyLocalV6, anyLocal, criteria);
        } catch (Exception e) {
            throw new StartException(e);
        }
        if (this.interfaceBinding == null) {
            throw ServerMessages.MESSAGES.failedToResolveInterface(name);
        }
        log.debugf("NetworkInterfaceService matched interface binding: %s\n", interfaceBinding);
    }

    public static NetworkInterfaceBinding createBinding(ParsedInterfaceCriteria criteria) throws SocketException,
            UnknownHostException {
        return createBinding(criteria.isAnyLocalV4(), criteria.isAnyLocalV6(), criteria.isAnyLocal(),
                new OverallInterfaceCriteria(null, criteria.getCriteria()));
    }

    static NetworkInterfaceBinding createBinding(final boolean anyLocalV4, final boolean anyLocalV6,
                                                 final boolean anyLocal, final OverallInterfaceCriteria criteria) throws SocketException, UnknownHostException {
        if (anyLocalV4) {
            return getNetworkInterfaceBinding(IPV4_ANYLOCAL);
        } else if (anyLocalV6) {
            return getNetworkInterfaceBinding(IPV6_ANYLOCAL);
        } else if (anyLocal) {
            return getNetworkInterfaceBinding(isPreferIPv4Stack() ? IPV4_ANYLOCAL : IPV6_ANYLOCAL);
        } else {
            return resolveInterface(criteria);
        }
    }

    public synchronized void stop(StopContext arg0) {
        this.interfaceBinding = null;
    }

    public synchronized NetworkInterfaceBinding getValue() throws IllegalStateException {
        final NetworkInterfaceBinding binding = this.interfaceBinding;
        if (binding == null) {
            throw new IllegalStateException();
        }
        return binding;
    }

    private static NetworkInterfaceBinding resolveInterface(final OverallInterfaceCriteria criteria) throws SocketException {
        NetworkInterfaceBinding result = null;
        final Map<NetworkInterface, Set<InetAddress>> candidates = new HashMap<NetworkInterface, Set<InetAddress>>();
        final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            storeAddresses(networkInterfaces.nextElement(), candidates);
        }
        Map<NetworkInterface, Set<InetAddress>> acceptable = criteria.getAcceptableAddresses(candidates);

        if (acceptable.size() == 1 && acceptable.values().iterator().next().size() == 1) {
            // single result
            Map.Entry<NetworkInterface, Set<InetAddress>> entry = acceptable.entrySet().iterator().next();
            InetAddress addr = entry.getValue().iterator().next();
            result = new NetworkInterfaceBinding(Collections.singleton(entry.getKey()), addr);
        }
        return result;
    }

    private static void storeAddresses(final NetworkInterface networkInterface, final Map<NetworkInterface, Set<InetAddress>> candidates) {
        final Enumeration<InetAddress> interfaceAddresses = networkInterface.getInetAddresses();
        Set<InetAddress> addresses = new HashSet<InetAddress>();
        candidates.put(networkInterface, addresses);
        while (interfaceAddresses.hasMoreElements()) {
            addresses.add(interfaceAddresses.nextElement());
        }
        final Enumeration<NetworkInterface> subInterfaces = networkInterface.getSubInterfaces();
        while (subInterfaces.hasMoreElements()) {
            storeAddresses(subInterfaces.nextElement(), candidates);
        }
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

    private static boolean isPreferIPv4Stack() {

        return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            @Override
            public Boolean run() {
                try {
                    return Boolean.getBoolean("java.net.preferIPv4Stack");
                } catch (Exception e) {
                    return Boolean.FALSE;
                }
            }
        });
    }
}
