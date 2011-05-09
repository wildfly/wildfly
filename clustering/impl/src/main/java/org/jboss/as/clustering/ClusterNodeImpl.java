/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.clustering;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.jgroups.Address;

/**
 * Replacement for a JG IpAddress that doesn't base its representation on the JG address but on the computed node name added to
 * the IPAddress instead. This is to avoid any problem in the cluster as some nodes may interpret a node name differently (IP
 * resolution, name case, FQDN or host name, etc.)
 *
 * @see org.jboss.ha.framework.server.ClusterPartitionMBean
 *
 * @author <a href="mailto:sacha.labourey@jboss.org">Sacha Labourey</a>.
 * @author Brian Stansberry
 * @author Vladimir Blagojevic
 * @author <a href="mailto:galder.zamarreno@jboss.com">Galder Zamarreno</a>
 * @version $Revision: 107153 $
 */

public class ClusterNodeImpl implements ClusterNode {
    // Constants -----------------------------------------------------

    /** The serialVersionUID */
    private static final long serialVersionUID = -1831036833785680731L;

    // Attributes ----------------------------------------------------

    private final String id;
    private final Address jgAddress;
    private final InetSocketAddress socketAddress;

    // Static --------------------------------------------------------

    // Constructors --------------------------------------------------

    ClusterNodeImpl(String id, Address jgAddress, InetSocketAddress socketAddress) {
        if (id == null) {
            throw new IllegalArgumentException("Null id");
        }
        if (socketAddress == null) {
            throw new IllegalArgumentException("Null addressPort");
        }
        this.id = id;
        this.socketAddress = socketAddress;

        this.jgAddress = jgAddress;

    }

    // Public --------------------------------------------------------

    @Override
    public String getName() {
        return this.id;
    }

    @Override
    public InetAddress getIpAddress() {
        return this.socketAddress.getAddress();
    }

    @Override
    public int getPort() {
        return this.socketAddress.getPort();
    }

    // Package protected ----------------------------------------------

    Address getOriginalJGAddress() {
        return this.jgAddress;
    }

    // Comparable implementation ----------------------------------------------

    @Override
    public int compareTo(ClusterNode o) {
        if (o == null)
            throw new ClassCastException("Comparison to null value");

        if (!(o instanceof ClusterNodeImpl))
            throw new ClassCastException("Comparison between different classes");

        return this.id.compareTo(o.getName());
    }

    // java.lang.Object overrides ---------------------------------------------------

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (!(obj instanceof ClusterNodeImpl))
            return false;

        ClusterNodeImpl other = (ClusterNodeImpl) obj;
        return this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return this.getName();
    }

    // Package protected ---------------------------------------------

    // Protected -----------------------------------------------------

    // Private -------------------------------------------------------

    // Inner classes -------------------------------------------------

}
