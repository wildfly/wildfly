/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.server;

import java.net.InetSocketAddress;

import org.wildfly.clustering.Node;

public class SimpleNode implements Node {
    private final String name;
    private final InetSocketAddress socketAddress;

    public SimpleNode(String name, InetSocketAddress socketAddress) {
        this.name = name;
        this.socketAddress = socketAddress;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public InetSocketAddress getSocketAddress() {
        return this.socketAddress;
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Node)) return false;
        Node node = (Node) object;
        return node.getName().equals(this.name);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public String toString() {
        return this.name;
    }
}
