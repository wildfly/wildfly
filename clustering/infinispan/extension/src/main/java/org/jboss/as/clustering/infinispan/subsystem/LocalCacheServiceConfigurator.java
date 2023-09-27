/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.configuration.cache.CacheMode;
import org.jboss.as.controller.PathAddress;

/**
 * @author Paul Ferraro
 */
public class LocalCacheServiceConfigurator extends CacheConfigurationServiceConfigurator {

    LocalCacheServiceConfigurator(PathAddress address) {
        super(address, CacheMode.LOCAL);
    }
}
