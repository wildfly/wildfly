package org.wildfly.clustering.server.infinispan.group;

import org.jgroups.Address;

/**
 * A object that is identified by a JGroups {@link Address}.
 * @author Paul Ferraro
 */
public interface Addressable {
    Address getAddress();
}
