/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering;

import java.util.ServiceLoader;

import org.jboss.logging.Logger;
import org.junit.Test;
import org.wildfly.clustering.server.service.DistributedCacheServiceConfiguratorProvider;
import org.wildfly.clustering.server.service.DistributedGroupServiceConfiguratorProvider;
import org.wildfly.clustering.server.service.IdentityCacheServiceConfiguratorProvider;
import org.wildfly.clustering.server.service.IdentityGroupServiceConfiguratorProvider;
import org.wildfly.clustering.server.service.LocalCacheServiceConfiguratorProvider;
import org.wildfly.clustering.server.service.LocalGroupServiceConfiguratorProvider;

/**
 * Validates loading of services.
 * @author Paul Ferraro
 */
public class ServiceLoaderTestCase {
    private static final Logger LOGGER = Logger.getLogger(ServiceLoaderTestCase.class);

    @Test
    public void load() {
        load(IdentityGroupServiceConfiguratorProvider.class);
        load(IdentityCacheServiceConfiguratorProvider.class);
        load(DistributedGroupServiceConfiguratorProvider.class);
        load(DistributedCacheServiceConfiguratorProvider.class);
        load(LocalGroupServiceConfiguratorProvider.class);
        load(LocalCacheServiceConfiguratorProvider.class);
    }

    private static <T> void load(Class<T> targetClass) {
        ServiceLoader.load(targetClass, ServiceLoaderTestCase.class.getClassLoader())
                .forEach(object -> LOGGER.tracef("\t" + object.getClass().getName()));
    }
}
