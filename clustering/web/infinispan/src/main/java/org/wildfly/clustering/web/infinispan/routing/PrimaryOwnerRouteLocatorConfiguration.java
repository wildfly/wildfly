/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.routing;

import org.infinispan.Cache;
import org.infinispan.remoting.transport.Address;
import org.wildfly.clustering.ee.infinispan.GroupedKey;
import org.wildfly.clustering.registry.Registry;
import org.wildfly.clustering.server.NodeFactory;

/**
 * @author Paul Ferraro
 */
public interface PrimaryOwnerRouteLocatorConfiguration {

    Registry<String, Void> getRegistry();

    Cache<GroupedKey<String>, ?> getCache();

    NodeFactory<Address> getMemberFactory();
}
