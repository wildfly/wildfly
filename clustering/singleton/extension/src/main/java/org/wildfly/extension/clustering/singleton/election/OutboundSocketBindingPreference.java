/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.clustering.singleton.election;

import java.net.UnknownHostException;
import java.util.function.Supplier;

import org.jboss.as.network.OutboundSocketBinding;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.singleton.election.Preference;

/**
 * An election policy preference defined as an outbound socket binding.
 * @author Paul Ferraro
 */
public class OutboundSocketBindingPreference implements Preference {

    private final Supplier<OutboundSocketBinding> binding;

    public OutboundSocketBindingPreference(Supplier<OutboundSocketBinding> binding) {
        this.binding = binding;
    }

    @Override
    public boolean preferred(Node node) {
        OutboundSocketBinding binding = this.binding.get();
        try {
            return binding.getResolvedDestinationAddress().equals(node.getSocketAddress().getAddress()) && (binding.getDestinationPort() == node.getSocketAddress().getPort());
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
