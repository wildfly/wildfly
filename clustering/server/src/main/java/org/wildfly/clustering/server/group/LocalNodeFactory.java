/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.server.group;

import java.util.Collection;

import org.jboss.as.clustering.jgroups.JChannelFactory;
import org.jboss.as.server.ServerEnvironment;
import org.jgroups.Address;
import org.wildfly.clustering.group.Node;

/**
 * Non-clustered {@link ChannelNodeFactory}.
 * @author Paul Ferraro
 */
public class LocalNodeFactory implements ChannelNodeFactory {

    private final String cluster;
    private final String name;

    public LocalNodeFactory(String cluster, ServerEnvironment environment) {
        this.cluster = cluster;
        this.name = JChannelFactory.createNodeName(cluster, environment);
    }

    @Override
    public Node createNode(Address address) {
        return new LocalNode(this.cluster, this.name);
    }

    @Override
    public void invalidate(Collection<Address> addresses) {
        // Do nothing
    }
}
