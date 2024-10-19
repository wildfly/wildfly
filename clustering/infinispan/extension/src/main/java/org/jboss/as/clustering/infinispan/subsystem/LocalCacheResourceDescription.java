/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.configuration.cache.CacheMode;
import org.jboss.as.controller.PathElement;
import org.wildfly.clustering.server.service.LocalCacheServiceInstallerProvider;

/**
 * @author Paul Ferraro
 *
 */
public enum LocalCacheResourceDescription implements CacheResourceDescription<LocalCacheServiceInstallerProvider> {
    INSTANCE;

    static PathElement pathElement(String name) {
        return PathElement.pathElement("local-cache", name);
    }

    private final PathElement path = pathElement(PathElement.WILDCARD_VALUE);

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public Class<LocalCacheServiceInstallerProvider> getProviderClass() {
        return LocalCacheServiceInstallerProvider.class;
    }

    @Override
    public CacheMode getCacheMode() {
        return CacheMode.LOCAL;
    }
}
