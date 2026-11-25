/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.web;

import org.wildfly.clustering.infinispan.client.service.HotRodCacheConfigurationAttributeGroup;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.CacheConfigurationAttributeGroup;
import org.wildfly.clustering.web.service.user.DistributableUserManagementProvider;
import org.wildfly.extension.clustering.web.sso.hotrod.HotRodUserManagementProvider;

/**
 * Registers a resource definition for a HotRod user management provider.
 * @author Paul Ferraro
 */
public class HotRodUserManagementResourceDefinitionRegistrar extends UserManagementResourceDefinitionRegistrar {

    static final CacheConfigurationAttributeGroup CACHE_ATTRIBUTE_GROUP = new HotRodCacheConfigurationAttributeGroup(CAPABILITY);

    HotRodUserManagementResourceDefinitionRegistrar() {
        super(UserManagementResourceRegistration.HOTROD, CACHE_ATTRIBUTE_GROUP);
    }

    @Override
    public DistributableUserManagementProvider apply(BinaryServiceConfiguration configuration) {
        return new HotRodUserManagementProvider(configuration);
    }
}
