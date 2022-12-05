package org.wildfly.clustering.server.infinispan.group;

import java.util.function.Function;

import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.jgroups.JGroupsAddressCache;
import org.wildfly.clustering.group.Node;

/**
 * Resolves the Infinispan {@link Address} of a {@link Node}.
 * @author Paul Ferraro
 */
public enum InfinispanAddressResolver implements Function<Node, Address> {
    INSTANCE;

    @Override
    public Address apply(Node node) {
        return JGroupsAddressCache.fromJGroupsAddress(JGroupsAddressResolver.INSTANCE.apply(node));
    }
}
