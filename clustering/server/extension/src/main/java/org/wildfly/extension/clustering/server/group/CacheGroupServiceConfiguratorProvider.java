/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.server.group;

import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.server.service.CacheCapabilityServiceConfiguratorFactory;
import org.wildfly.clustering.server.service.ClusteringCacheRequirement;
import org.wildfly.extension.clustering.server.CacheRequirementServiceConfiguratorProvider;

/**
 * Provides the requisite builders for a cache-based {@link Group}.
 * @author Paul Ferraro
 */
public class CacheGroupServiceConfiguratorProvider extends CacheRequirementServiceConfiguratorProvider<Group> {

    protected CacheGroupServiceConfiguratorProvider(CacheCapabilityServiceConfiguratorFactory<Group> factory) {
        super(ClusteringCacheRequirement.GROUP, factory);
    }
}
