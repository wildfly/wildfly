/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan;

import java.util.ServiceLoader;

import org.infinispan.persistence.keymappers.TwoWayKey2StringMapper;
import org.infinispan.protostream.SerializationContextInitializer;
import org.junit.Test;
import org.wildfly.clustering.marshalling.Externalizer;
import org.wildfly.clustering.server.service.DistributedCacheServiceConfiguratorProvider;
import org.wildfly.clustering.server.service.IdentityCacheServiceConfiguratorProvider;
import org.wildfly.clustering.server.service.LocalCacheServiceConfiguratorProvider;

/**
 * Validates loading of services.
 *
 * @author Paul Ferraro
 */
public class ServiceLoaderTestCase {

    private static <T> void load(Class<T> targetClass) {
        System.out.println(targetClass.getName() + ":");
        ServiceLoader.load(targetClass, ServiceLoaderTestCase.class.getClassLoader()).forEach(object -> System.out.println("\t" + object.getClass().getName()));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void load() {
        load(Externalizer.class);
        load(org.wildfly.clustering.ejb.bean.LegacyBeanManagementProviderFactory.class);
        load(DistributedCacheServiceConfiguratorProvider.class);
        load(LocalCacheServiceConfiguratorProvider.class);
        load(IdentityCacheServiceConfiguratorProvider.class);
        load(TwoWayKey2StringMapper.class);
        load(SerializationContextInitializer.class);
    }
}
