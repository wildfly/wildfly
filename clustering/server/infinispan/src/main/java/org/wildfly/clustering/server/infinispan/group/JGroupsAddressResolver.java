package org.wildfly.clustering.server.infinispan.group;

import java.util.function.Function;

import org.jgroups.Address;
import org.wildfly.clustering.group.Node;

/**
 * Resolves the JGroups {@link Address} of a {@link Node}.
 * @author Paul Ferraro
 */
public enum JGroupsAddressResolver implements Function<Node, Address> {
    INSTANCE;

    @Override
    public Address apply(Node node) {
        if (!(node instanceof Addressable)) {
            throw new IllegalArgumentException(node.toString());
        }
        return ((Addressable) node).getAddress();
    }
}
