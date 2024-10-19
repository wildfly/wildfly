/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

/**
 * Registers a resource definition for a local cache.
 * @author Paul Ferraro
 */
public class LocalCacheResourceDefinitionRegistrar extends CacheResourceDefinitionRegistrar {

    LocalCacheResourceDefinitionRegistrar() {
        super(CacheResourceRegistration.LOCAL);
    }
}
