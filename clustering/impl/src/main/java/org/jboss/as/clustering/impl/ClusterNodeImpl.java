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
package org.jboss.as.clustering.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.jboss.as.clustering.ClusterNode;
import org.jgroups.Address;

import static org.jboss.as.clustering.impl.ClusteringImplMessages.MESSAGES;

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
    private transient Address jgAddress;
    private final InetSocketAddress socketAddress;

    // Static --------------------------------------------------------

    // Constructors --------------------------------------------------

    ClusterNodeImpl(String id, Address jgAddress, InetSocketAddress socketAddress) {
        if (id == null) {
            throw MESSAGES.nullVar("id");
        }
        if (socketAddress == null) {
            throw MESSAGES.nullVar("addressPort");
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

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeUTF(this.jgAddress.getClass().getName());
        try {
            this.jgAddress.writeTo(out);
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        try {
            this.jgAddress = Address.class.getClassLoader().loadClass(in.readUTF()).asSubclass(Address.class).newInstance();
            this.jgAddress.readFrom(in);
        } catch (IOException e) {
            throw e;
        } catch (ClassNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
