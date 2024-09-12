/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.dispatcher.legacy;

import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.server.infinispan.CacheContainerGroupMember;
import org.wildfly.clustering.server.infinispan.dispatcher.CacheContainerCommandDispatcherFactory;
import org.wildfly.extension.clustering.server.group.legacy.LegacyCacheContainerGroup;
import org.wildfly.extension.clustering.server.group.legacy.LegacyGroup;

/**
 * @author Paul Ferraro
 */
@Deprecated
public interface LegacyCacheContainerCommandDispatcherFactory extends LegacyCommandDispatcherFactory<Address, CacheContainerGroupMember> {

    @Override
    CacheContainerCommandDispatcherFactory unwrap();

    @Override
    default LegacyGroup<Address, CacheContainerGroupMember> getGroup() {
        return LegacyCacheContainerGroup.wrap(this.unwrap().getGroup());
    }

    static LegacyCacheContainerCommandDispatcherFactory wrap(CacheContainerCommandDispatcherFactory factory) {
        return () -> factory;
    }
}
