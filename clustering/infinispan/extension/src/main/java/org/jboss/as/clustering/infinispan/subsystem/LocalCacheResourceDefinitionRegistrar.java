/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import org.wildfly.clustering.server.service.LocalCacheServiceInstallerProvider;

/**
 * Registers a resource definition for a local cache.
 * @author Paul Ferraro
 */
public class LocalCacheResourceDefinitionRegistrar extends CacheResourceDefinitionRegistrar<LocalCacheServiceInstallerProvider> {

    LocalCacheResourceDefinitionRegistrar() {
        super(new Configurator<>() {
            @Override
            public CacheResourceRegistration getResourceRegistration() {
                return CacheResourceRegistration.LOCAL;
            }

            @Override
            public Class<LocalCacheServiceInstallerProvider> getServiceInstallerProviderClass() {
                return LocalCacheServiceInstallerProvider.class;
            }
        });
    }
}
