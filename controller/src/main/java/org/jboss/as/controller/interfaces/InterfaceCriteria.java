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

/**
 *
 */
package org.jboss.as.controller.interfaces;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Map;
import java.util.Set;

/**
 * A criteria used to determine what IP address to use for an interface.
 *
 * @author Brian Stansberry
 */
public interface InterfaceCriteria extends Serializable {

    /**
     * Gets which of the available network interfaces and addresses are acceptable for
     * use. Acceptance is indicated by including a network interface and the acceptable addresses associated
     * with it in a map. The map may include more than one entry, and the set of addresses for any given
     * entry may include more than one value. For those criteria which override the configured addresses (e.g.
     * {@link LoopbackAddressInterfaceCriteria}, the override
     * address should be returned in the set associated with the relevant interface.
     *
     * @param candidates map of candidate interfaces and addresses. This map may include all known interfaces and
     *                   addresses or the system, or a subset of them that were acceptable to other criteria.
     *
     * @return map of accepted network interfaces to their acceptable addresses. Cannot return {@code null}; an
     *         empty map should be returned if no acceptable items are found. The set of addresses stored as
     *         values in the map should not be {@code null} or empty; no key for an interface should be stored
     *         if no addresses are acceptable. A criteria that only cares about the network interface should
     *         return a map including all provided candidate addresses for that interface.
     *
     * @throws SocketException
     */
    Map<NetworkInterface, Set<InetAddress>> getAcceptableAddresses(final Map<NetworkInterface, Set<InetAddress>> candidates) throws SocketException;
}
