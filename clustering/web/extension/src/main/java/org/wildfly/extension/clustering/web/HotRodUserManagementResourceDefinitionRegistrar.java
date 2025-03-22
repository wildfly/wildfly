/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.web;

import org.wildfly.clustering.infinispan.client.service.HotRodCacheConfigurationDescriptor;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.CacheConfigurationDescriptor;
import org.wildfly.clustering.web.service.user.DistributableUserManagementProvider;
import org.wildfly.extension.clustering.web.sso.hotrod.HotRodUserManagementProvider;

/**
 * Registers a resource definition for a HotRod user management provider.
 * @author Paul Ferraro
 */
public class HotRodUserManagementResourceDefinitionRegistrar extends UserManagementResourceDefinitionRegistrar {

    static final CacheConfigurationDescriptor CACHE_CONFIGURATION = new HotRodCacheConfigurationDescriptor(CAPABILITY);

    HotRodUserManagementResourceDefinitionRegistrar() {
        super(UserManagementResourceRegistration.HOTROD, CACHE_CONFIGURATION);
    }

    @Override
    public DistributableUserManagementProvider apply(BinaryServiceConfiguration configuration) {
        return new HotRodUserManagementProvider(configuration);
    }
}
