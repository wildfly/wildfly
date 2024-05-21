/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group.legacy;

import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.group.Node;
import org.wildfly.clustering.server.infinispan.CacheContainerGroup;
import org.wildfly.clustering.server.infinispan.CacheContainerGroupMember;

/**
 * @author Paul Ferraro
 */
@Deprecated
public interface LegacyCacheContainerGroup extends LegacyGroup<Address, CacheContainerGroupMember> {

    @Override
    CacheContainerGroup unwrap();

    @Override
    default Node wrap(CacheContainerGroupMember member) {
        return LegacyCacheContainerGroupMember.wrap(member);
    }

    @Override
    default CacheContainerGroupMember unwrap(Node node) {
        return ((LegacyCacheContainerGroupMember) node).unwrap();
    }

    static LegacyCacheContainerGroup wrap(CacheContainerGroup group) {
        return new LegacyCacheContainerGroup() {
            @Override
            public CacheContainerGroup unwrap() {
                return group;
            }
        };
    }
}
