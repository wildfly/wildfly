/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.web;

import org.wildfly.clustering.infinispan.service.InfinispanCacheConfigurationAttributeGroup;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.clustering.server.service.CacheConfigurationAttributeGroup;
import org.wildfly.clustering.web.service.user.DistributableUserManagementProvider;
import org.wildfly.extension.clustering.web.sso.infinispan.InfinispanUserManagementProvider;

/**
 * Registers a resource definition for an Infinispan user management provider.
 * @author Paul Ferraro
 */
public class InfinispanUserManagementResourceDefinitionRegistrar extends UserManagementResourceDefinitionRegistrar {

    static final CacheConfigurationAttributeGroup CACHE_ATTRIBUTE_GROUP = new InfinispanCacheConfigurationAttributeGroup(CAPABILITY);

    InfinispanUserManagementResourceDefinitionRegistrar() {
        super(UserManagementResourceRegistration.INFINISPAN, CACHE_ATTRIBUTE_GROUP);
    }

    @Override
    public DistributableUserManagementProvider apply(BinaryServiceConfiguration configuration) {
        return new InfinispanUserManagementProvider(configuration);
    }
}
