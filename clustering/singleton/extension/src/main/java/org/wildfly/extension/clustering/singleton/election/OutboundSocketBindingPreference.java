/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
