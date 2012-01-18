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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.interfaces.InterfaceCriteria;
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

    private static final boolean preferIPv4Stack = Boolean.getBoolean("java.net.preferIPv4Stack");
    private static final boolean preferIPv6Stack = Boolean.getBoolean("java.net.preferIPv6Addresses");

    private static final String IPV4_ANYLOCAL = "0.0.0.0";
    private static final String IPV6_ANYLOCAL = "::";

    /** The interface binding. */
    private NetworkInterfaceBinding interfaceBinding;

    private final String name;
    private final boolean anyLocalV4;
    private final boolean anyLocalV6;
    private final boolean anyLocal;
    private final InterfaceCriteria criteria;

    public static Service<NetworkInterfaceBinding> create(String name, ParsedInterfaceCriteria criteria) {
        return new NetworkInterfaceService(name, criteria.isAnyLocalV4(), criteria.isAnyLocalV6(), criteria.isAnyLocal(),
                new OverallInterfaceCriteria(criteria.getCriteria()));
    }

    public NetworkInterfaceService(final String name, final boolean anyLocalV4, final boolean anyLocalV6,
            final boolean anyLocal, final InterfaceCriteria criteria) {
        this.name = name;
        this.anyLocalV4 = anyLocalV4;
        this.anyLocalV6 = anyLocalV6;
        this.anyLocal = anyLocal;
        this.criteria = criteria;
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
                new OverallInterfaceCriteria(criteria.getCriteria()));
    }

    static NetworkInterfaceBinding createBinding(final boolean anyLocalV4, final boolean anyLocalV6,
                                                 final boolean anyLocal, final InterfaceCriteria criteria) throws SocketException, UnknownHostException {
        if (anyLocalV4) {
            return getNetworkInterfaceBinding(IPV4_ANYLOCAL);
        } else if (anyLocalV6) {
            return getNetworkInterfaceBinding(IPV6_ANYLOCAL);
        } else if (anyLocal) {
            return getNetworkInterfaceBinding(preferIPv4Stack ? IPV4_ANYLOCAL : IPV6_ANYLOCAL);
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

    static NetworkInterfaceBinding resolveInterface(final InterfaceCriteria criteria) throws SocketException {
        NetworkInterfaceBinding result = null;
        final Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        log.tracef("resolveInterface, checking criteria: %s\n", criteria);
        while (result == null && networkInterfaces.hasMoreElements()) {
            final NetworkInterface networkInterface = networkInterfaces.nextElement();
            result = resolveInterface(criteria, networkInterface);
            if (result == null) {
                final Enumeration<NetworkInterface> subInterfaces = networkInterface.getSubInterfaces();
                while (result == null && subInterfaces.hasMoreElements()) {
                    final NetworkInterface subInterface = subInterfaces.nextElement();
                    result = resolveInterface(criteria, subInterface);
                }
            }
        }
        return result;
    }

    private static NetworkInterfaceBinding resolveInterface(final InterfaceCriteria criteria,
            final NetworkInterface networkInterface) throws SocketException {
        log.tracef("resolveInterface, checking NetworkInterface: %s\n", toString(networkInterface));
        final Enumeration<InetAddress> interfaceAddresses = networkInterface.getInetAddresses();
        while (interfaceAddresses.hasMoreElements()) {
            final InetAddress address = interfaceAddresses.nextElement();
            if (preferIPv4Stack && !preferIPv6Stack && !(address instanceof Inet4Address)) {
                continue;
            } else if (preferIPv6Stack && !preferIPv4Stack && !(address instanceof Inet6Address)) {
                continue;
            }
            log.tracef("Checking interface(name=%s,address=%s), criteria=%s\n", networkInterface.getName(), address, criteria);
            InetAddress bindAddress = criteria.isAcceptable(networkInterface, address);
            if (bindAddress != null) {
                log.tracef("Criteria provided bind address: %s\n", bindAddress);
                return new NetworkInterfaceBinding(Collections.singleton(networkInterface), bindAddress);
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

    /** Overall interface criteria. */
    static final class OverallInterfaceCriteria implements InterfaceCriteria {
        private static final long serialVersionUID = -5417786897309925997L;
        private final Set<InterfaceCriteria> interfaceCriteria;

        public OverallInterfaceCriteria(Set<InterfaceCriteria> criteria) {
            interfaceCriteria = criteria;
        }

        /**
         * Loop through associated criteria and build a unique address as follows: 1. Iterate through all criteria, returning
         * null if any criteria return a null result from
         * {@linkplain #isAcceptable(java.net.NetworkInterface, java.net.InetAddress)}. 2. Collect the accepted addressed into a
         * Set. 3. If the set contains a single address, this is returned as the criteria match. 4. If there are more than 2
         * addresses, log a warning and return null to indicate no match was agreed on. 5. If there are 2 addresses, remove the
         * input address and if the resulting set has only one address, return it as the criteria match. Otherwise, log a
         * warning indicating 2 unique criteria addresses were seen and return null to indicate no match was agreed on.
         *
         * @return the unique address determined by the all criteria, null if no such address was found
         * @throws SocketException
         */
        @Override
        public InetAddress isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException {
            // Build up a set of unique addresses from the criteria
            HashSet<InetAddress> addresses = new HashSet<InetAddress>();
            for (InterfaceCriteria criteria : interfaceCriteria) {
                InetAddress bindAddress = criteria.isAcceptable(networkInterface, address);
                if (bindAddress == null) {
                    log.debugf("Criteria(%s) failed to accept input\n", criteria);
                    return null;
                }
                log.tracef("%s accepted input, provided bind address: %s", criteria, bindAddress);
                addresses.add(bindAddress);
            }
            log.debugf("Candidate accepted addresses are: %s\n", addresses);
            // Determine which address to return
            InetAddress bindAddress = null;
            if (addresses.size() > 0) {
                log.tracef("Determining unique address from among: %s\n", addresses.toString());
                if (addresses.size() == 1)
                    bindAddress = addresses.iterator().next();
                else {
                    // Remove the input address and see if non-unique addresses exist
                    if (addresses.size() > 2)
                        ServerLogger.ROOT_LOGGER.moreThanTwoUniqueCriteria(addresses.toString());
                    else {
                        ServerLogger.ROOT_LOGGER.checkingTwoUniqueCriteria(addresses.toString());
                        addresses.remove(address);
                        if (addresses.size() == 1)
                            bindAddress = addresses.iterator().next();
                        else
                            ServerLogger.ROOT_LOGGER.twoUniqueCriteriaAddresses(addresses.toString());
                    }
                }
            }
            return bindAddress;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("OverallInterfaceCriteria(");
            for (InterfaceCriteria criteria : interfaceCriteria) {
                sb.append(criteria.toString());
                sb.append(",");
            }
            sb.setLength(sb.length() - 1);
            sb.append(")");
            return sb.toString();
        }
    }

    /**
     * A little toString utility for NetworkInterface since its version seems to add the hardware address and this corrupts
     * logging output.
     *
     * @param iface The interface to convert to string format.
     * @return toString for NetworkInterface
     */
    static String toString(NetworkInterface iface) {
        StringBuilder sb = new StringBuilder("NetworkInterface(");
        sb.append("name:");
        sb.append(iface.getName());
        sb.append("(");
        sb.append(iface.getDisplayName());
        sb.append("), addresses:");
        sb.append(iface.getInterfaceAddresses());
        return sb.toString();
    }
}
