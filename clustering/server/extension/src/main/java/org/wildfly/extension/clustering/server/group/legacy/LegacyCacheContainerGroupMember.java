/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group.legacy;

import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.server.infinispan.CacheContainerGroupMember;

/**
 * @author Paul Ferraro
 */
@Deprecated
public interface LegacyCacheContainerGroupMember extends LegacyGroupMember<Address> {

    @Override
    CacheContainerGroupMember unwrap();

    static LegacyCacheContainerGroupMember wrap(CacheContainerGroupMember member) {
        return () -> member;
    }
}
