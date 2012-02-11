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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.dmr.ModelNode;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Tests for AS7-3041.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class IPv6ScopeIdMatchUnitTestCase {

    private static NetworkInterface loopbackInterface;
    private static Inet6Address loopbackAddress;
    private static Map<NetworkInterface, Set<Inet6Address>> addresses = new HashMap<NetworkInterface, Set<Inet6Address>>();

    @BeforeClass
    public static void setup() throws Exception {
        Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
        while (nifs.hasMoreElements()) {
            NetworkInterface nif = nifs.nextElement();
            processNetworkInterface(nif);
            Enumeration<NetworkInterface> subs = nif.getSubInterfaces();
            while (subs.hasMoreElements()) {
                NetworkInterface sub = subs.nextElement();
                processNetworkInterface(sub);
            }
        }
        System.out.println("loopback = " + loopbackAddress);
        for (Map.Entry<NetworkInterface, Set<Inet6Address>> entry : addresses.entrySet()) {
            System.out.println(entry.getKey() + " " + entry.getValue());
        }

    }

    private static void processNetworkInterface(NetworkInterface nif) {
        for (InterfaceAddress ifaddr : nif.getInterfaceAddresses()) {
            InetAddress inetAddress = ifaddr.getAddress();
            if (inetAddress instanceof Inet6Address) {
                Inet6Address inet6 = (Inet6Address) inetAddress;
                if (inet6.isLoopbackAddress()) {
                    loopbackInterface = nif;
                    loopbackAddress = inet6;
                } else if (addresses.containsKey(nif)) {
                    addresses.get(nif).add(inet6);
                } else {
                    Set<Inet6Address> set = new HashSet<Inet6Address>();
                    set.add(inet6);
                    addresses.put(nif, set);
                }
            }
        }
    }

    @Test
    public void testLoopback() throws Exception {
        if (loopbackAddress == null) {
            return;
        }

        InetAddressMatchInterfaceCriteria criteria = new InetAddressMatchInterfaceCriteria(new ModelNode("::1"));
        assertEquals(loopbackAddress, criteria.isAcceptable(loopbackInterface, loopbackAddress));
        criteria = new InetAddressMatchInterfaceCriteria(new ModelNode("::1%" + loopbackInterface.getName()));
        if (loopbackAddress.getScopeId() > 0) {
            assertEquals(loopbackAddress, criteria.isAcceptable(loopbackInterface, loopbackAddress));
        } else {
            // This match fails because ::1%lo becomes ::1%<number_of_lo>
            assertNull(criteria.isAcceptable(loopbackInterface, loopbackAddress));
        }
        criteria = new InetAddressMatchInterfaceCriteria(new ModelNode("::1%" + loopbackAddress.getScopeId()));
        assertEquals(loopbackAddress, criteria.isAcceptable(loopbackInterface, loopbackAddress));
        criteria = new InetAddressMatchInterfaceCriteria(new ModelNode("::1%" + (loopbackAddress.getScopeId() + 1)));
        assertNull(criteria.isAcceptable(loopbackInterface, loopbackAddress));
    }

    @Test
    public void testNonLoopback() throws Exception {

        for (Map.Entry<NetworkInterface, Set<Inet6Address>> entry : addresses.entrySet()) {
            NetworkInterface nif = entry.getKey();
            for (Inet6Address address : entry.getValue()) {
                String hostAddress = address.getHostAddress();
                int pos = hostAddress.indexOf('%');
                if (pos > -1) {
                    hostAddress = hostAddress.substring(0, pos);
                }
                InetAddressMatchInterfaceCriteria criteria = new InetAddressMatchInterfaceCriteria(new ModelNode(hostAddress));
                assertEquals(address, criteria.isAcceptable(nif, address));
                criteria = new InetAddressMatchInterfaceCriteria(new ModelNode(hostAddress + "%" + nif.getName()));
                assertEquals(address, criteria.isAcceptable(nif, address));
                criteria = new InetAddressMatchInterfaceCriteria(new ModelNode(hostAddress + "%" + address.getScopeId()));
                assertEquals(address, criteria.isAcceptable(nif, address));
                criteria = new InetAddressMatchInterfaceCriteria(new ModelNode(hostAddress + "%" + (address.getScopeId() + 1)));
                assertNull(criteria.isAcceptable(nif, address));
                criteria = new InetAddressMatchInterfaceCriteria(new ModelNode(hostAddress + "%bogus"));
                assertNull(criteria.isAcceptable(nif, address));
            }
        }
    }
}
