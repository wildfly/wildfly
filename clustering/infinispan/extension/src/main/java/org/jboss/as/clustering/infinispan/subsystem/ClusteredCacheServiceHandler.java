/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.wildfly.clustering.server.service.DistributedCacheServiceConfiguratorProvider;

/**
 * @author Paul Ferraro
 */
public class ClusteredCacheServiceHandler extends CacheServiceHandler<DistributedCacheServiceConfiguratorProvider> {

    ClusteredCacheServiceHandler(ResourceServiceConfiguratorFactory configuratorFactory) {
        super(configuratorFactory, DistributedCacheServiceConfiguratorProvider.class);
    }
}
