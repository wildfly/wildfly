/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.configuration.cache.CacheMode;
import org.jboss.as.controller.PathAddress;

/**
 * Scattered cache is no longer supported upstream.
 * This just configures a distributed cache configuration with 2 owners.
 * @author Paul Ferraro
 */
@Deprecated
public class ScatteredCacheServiceConfigurator extends SegmentedCacheServiceConfigurator {

    ScatteredCacheServiceConfigurator(PathAddress address) {
        super(address, CacheMode.DIST_SYNC);
    }
}
