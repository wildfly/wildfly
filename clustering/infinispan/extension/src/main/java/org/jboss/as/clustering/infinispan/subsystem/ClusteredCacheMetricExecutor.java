/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.Cache;
import org.infinispan.remoting.rpc.RpcManagerImpl;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Handler for clustered cache metrics.
 *
 * @author Paul Ferraro
 */
public class ClusteredCacheMetricExecutor extends CacheMetricExecutor<RpcManagerImpl> {

    public ClusteredCacheMetricExecutor(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(executors);
    }

    @Override
    public RpcManagerImpl apply(Cache<?, ?> cache) {
        return (RpcManagerImpl) cache.getAdvancedCache().getRpcManager();
    }
}
