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
import java.util.Map;
import java.util.Set;

/**
 * {@link InterfaceCriteria} Placeholder interface criteria; enables support of wildcard addresses for inet-address.
 *
 * @author Mike Dobozy (mike.dobozy@amentra.com)
 *
 */
public class WildcardInetAddressInterfaceCriteria implements InterfaceCriteria {

    private static final long serialVersionUID = -4805776607639567774L;

    private Version version = Version.ANY;

    public enum Version {
        V4, V6, ANY
    }

    public WildcardInetAddressInterfaceCriteria(InetAddress address) {
        if (address instanceof Inet4Address) {
            version = Version.V4;
        }
        else if (address instanceof Inet6Address) {
            version = Version.V6;
        }
    }

    public Version getVersion() {
        return version;
    }

    @Override
    public Map<NetworkInterface, Set<InetAddress>> getAcceptableAddresses(Map<NetworkInterface, Set<InetAddress>> candidates) throws SocketException {
        return Collections.emptyMap();
    }

    @Override
    public int hashCode() {
        return version.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof WildcardInetAddressInterfaceCriteria)
                && version == ((WildcardInetAddressInterfaceCriteria)o).version;
    }
}
