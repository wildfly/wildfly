/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.Cache;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * @author Paul Ferraro
 *
 */
public class ReplicatedCacheResourceDefinitionRegistrar extends SharedStateCacheResourceDefinitionRegistrar {

    ReplicatedCacheResourceDefinitionRegistrar(FunctionExecutorRegistry<Cache<?, ?>> executors) {
        super(new Configurator() {
            @Override
            public CacheResourceRegistration getResourceRegistration() {
                return CacheResourceRegistration.REPLICATED;
            }
        }, executors);
    }
}
