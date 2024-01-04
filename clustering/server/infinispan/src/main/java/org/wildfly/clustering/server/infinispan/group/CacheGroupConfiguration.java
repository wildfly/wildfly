/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.server.infinispan.group;

import org.jgroups.Address;
import org.wildfly.clustering.ee.infinispan.InfinispanConfiguration;
import org.wildfly.clustering.server.NodeFactory;

/**
 * Configuration for a {@link CacheGroup}.
 * @author Paul Ferraro
 */
public interface CacheGroupConfiguration extends InfinispanConfiguration {
    NodeFactory<Address> getMemberFactory();
}
