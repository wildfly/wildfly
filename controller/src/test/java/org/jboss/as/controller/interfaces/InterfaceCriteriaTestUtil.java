/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utilities for interface criteria testing.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class InterfaceCriteriaTestUtil {

    static final Set<NetworkInterface> loopbackInterfaces;
    static final Set<NetworkInterface> nonLoopBackInterfaces;
    static final Set<NetworkInterface> allInterfaces;
    static final Map<NetworkInterface, Set<InetAddress>> allCandidates;

    private static final boolean preferIPv4Stack = Boolean.getBoolean("java.net.preferIPv4Stack");
    private static final boolean preferIPv6Stack = Boolean.getBoolean("java.net.preferIPv6Addresses");

    static {

        Set<NetworkInterface> loop = new HashSet<NetworkInterface>();
        Set<NetworkInterface> notLoop = new HashSet<NetworkInterface>();
        Set<NetworkInterface> all = new HashSet<NetworkInterface>();
        Map<NetworkInterface, Set<InetAddress>> candidates = new HashMap<NetworkInterface, Set<InetAddress>>();

        try {
            Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
            while (nics.hasMoreElements()) {
                NetworkInterface nic = nics.nextElement();
                all.add(nic);
                if (nic.isLoopback()) {
                    loop.add(nic);
                } else {
                    notLoop.add(nic);
                }
                Set<InetAddress> addresses = new HashSet<InetAddress>();
                Enumeration<InetAddress> addressEnumeration = nic.getInetAddresses();
                while (addressEnumeration.hasMoreElements()) {
                    addresses.add(addressEnumeration.nextElement());
                }
                if (addresses.size() > 0) {
                    candidates.put(nic, Collections.unmodifiableSet(addresses));
                }else{
                    notLoop.remove(nic);
                }

                Enumeration<NetworkInterface> subs = nic.getSubInterfaces();
                while (subs.hasMoreElements()) {
                    nic = subs.nextElement();
                    all.add(nic);
                    if (nic.isLoopback()) {
                        loop.add(nic);
                    } else {
                        notLoop.add(nic);
                    }
                    addresses = new HashSet<InetAddress>();
                    addressEnumeration = nic.getInetAddresses();
                    while (addressEnumeration.hasMoreElements()) {
                        addresses.add(addressEnumeration.nextElement());
                    }
                    if (addresses.size() > 0) {
                        candidates.put(nic, Collections.unmodifiableSet(addresses));
                    }else{
                        notLoop.remove(nic);
                    }

                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }

        loopbackInterfaces = Collections.unmodifiableSet(loop);
        nonLoopBackInterfaces = Collections.unmodifiableSet(notLoop);
        allInterfaces = Collections.unmodifiableSet(all);
        allCandidates = Collections.unmodifiableMap(candidates);
    }

    static Set<InetAddress> getRightTypeAddresses(Set<InetAddress> all) {
        Set<InetAddress> result = new HashSet<InetAddress>();
        for (InetAddress address : all) {

            if (preferIPv4Stack && !preferIPv6Stack && !(address instanceof Inet4Address)) {
                continue;
            } else if (preferIPv6Stack && !preferIPv4Stack && !(address instanceof Inet6Address)) {
                continue;
            }
            result.add(address);
        }
        return result;
    }
}
