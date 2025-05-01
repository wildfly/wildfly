/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

/**
 * Registers a resource definition for an invalidation cache.
 * @author Paul Ferraro
 */
public class InvalidationCacheResourceDefinitionRegistrar extends ClusteredCacheResourceDefinitionRegistrar {

    InvalidationCacheResourceDefinitionRegistrar() {
        super(CacheResourceRegistration.INVALIDATION);
    }
}
