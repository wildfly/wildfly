/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.wildfly.clustering.server.service.LocalCacheServiceConfiguratorProvider;


/**
 * @author Paul Ferraro
 */
public class LocalCacheServiceHandler extends CacheServiceHandler<LocalCacheServiceConfiguratorProvider> {

    LocalCacheServiceHandler() {
        super(LocalCacheServiceConfigurator::new, LocalCacheServiceConfiguratorProvider.class);
    }
}
