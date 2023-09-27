/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.server.group;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.group.Group;
import org.wildfly.clustering.server.service.LocalCacheServiceConfiguratorProvider;

/**
 * Provides the requisite builders for a non-clustered cache-based {@link Group} service.
 * @author Paul Ferraro
 */
@MetaInfServices({ LocalCacheServiceConfiguratorProvider.class, org.wildfly.clustering.server.service.group.LocalCacheGroupServiceConfiguratorProvider.class })
public class LocalCacheGroupServiceConfiguratorProvider extends CacheGroupServiceConfiguratorProvider implements org.wildfly.clustering.server.service.group.LocalCacheGroupServiceConfiguratorProvider {

    public LocalCacheGroupServiceConfiguratorProvider() {
        super((name, containerName, cacheName) -> new LocalGroupServiceConfigurator(name));
    }
}
