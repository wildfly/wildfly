/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.interfaces;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Abstract superclass for {@link InterfaceCriteria} implementations.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public abstract class AbstractInterfaceCriteria implements InterfaceCriteria {

    private static final long serialVersionUID = -4266469792905191837L;

    /**
     * Gets whether the given network interface and address are acceptable for
     * use. Acceptance is indicated by returning the address which should be
     * used for binding against the network interface; typically this is the given {@code address}
     * parameter. For those criteria which override the configured address, the override address should
     * be returned.
     *
     * @param networkInterface the network interface. Cannot be <code>null</code>
     * @param address an address that is associated with <code>networkInterface</code>.
     * Cannot be <code>null</code>
     * @return <code>InetAddress</code> the non-null address to bind to if the
     * criteria is met, {@code null} if the criteria is not satisfied
     *
     * @throws SocketException if evaluating the state of {@code networkInterface} results in one
     */
    protected abstract InetAddress isAcceptable(NetworkInterface networkInterface, InetAddress address) throws SocketException;

    public Map<NetworkInterface, Set<InetAddress>> getAcceptableAddresses(final Map<NetworkInterface, Set<InetAddress>> candidates) throws SocketException {

        Map<NetworkInterface, Set<InetAddress>> result = new HashMap<NetworkInterface, Set<InetAddress>>();
        for (Map.Entry<NetworkInterface, Set<InetAddress>> entry : candidates.entrySet()) {
            NetworkInterface ni = entry.getKey();
            HashSet addresses = null;
            for (InetAddress address : entry.getValue()) {
                InetAddress accepted = isAcceptable(ni, address);
                if (accepted != null) {
                    if (addresses == null) {
                        addresses = new HashSet<InetAddress>();
                        result.put(ni, addresses);
                    }
                    addresses.add(accepted);
                }
            }
        }

        return result;
    }

    public static Map<NetworkInterface, Set<InetAddress>> cloneCandidates(final Map<NetworkInterface, Set<InetAddress>> candidates) {
        final Map<NetworkInterface, Set<InetAddress>> clone = new LinkedHashMap<NetworkInterface, Set<InetAddress>>();

        for (Map.Entry<NetworkInterface, Set<InetAddress>> entry : candidates.entrySet()) {
            clone.put(entry.getKey(), new LinkedHashSet<InetAddress>(entry.getValue()));
        }
        return clone;
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
