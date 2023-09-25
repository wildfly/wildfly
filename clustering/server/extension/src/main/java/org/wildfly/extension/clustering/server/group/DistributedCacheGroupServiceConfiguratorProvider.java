/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.server.service.DistributedCacheServiceConfiguratorProvider;

/**
 * Provides the requisite builders for a clustered cache-based {@link org.wildfly.clustering.group.Group} service.
 * @author Paul Ferraro
 */
@MetaInfServices({ DistributedCacheServiceConfiguratorProvider.class, org.wildfly.clustering.server.service.group.DistributedCacheGroupServiceConfiguratorProvider.class })
public class DistributedCacheGroupServiceConfiguratorProvider extends CacheGroupServiceConfiguratorProvider implements org.wildfly.clustering.server.service.group.DistributedCacheGroupServiceConfiguratorProvider {

    public DistributedCacheGroupServiceConfiguratorProvider() {
        super(CacheGroupServiceConfigurator::new);
    }
}
