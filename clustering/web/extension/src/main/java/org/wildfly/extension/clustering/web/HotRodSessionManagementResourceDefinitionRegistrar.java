/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.web;

import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.infinispan.client.service.HotRodCacheConfigurationAttributeGroup;
import org.wildfly.clustering.server.service.CacheConfigurationAttributeGroup;
import org.wildfly.extension.clustering.web.session.hotrod.HotRodSessionManagementProvider;
import org.wildfly.subsystem.resource.ResourceDescriptor;

/**
 * Registers a resource definition for a HotRod session management provider.
 * @author Paul Ferraro
 */
public class HotRodSessionManagementResourceDefinitionRegistrar extends SessionManagementResourceDefinitionRegistrar {

    static final CacheConfigurationAttributeGroup CACHE_ATTRIBUTE_GROUP = new HotRodCacheConfigurationAttributeGroup(CAPABILITY);

    static final AttributeDefinition EXPIRATION_THREAD_POOL_SIZE = new SimpleAttributeDefinitionBuilder("expiration-thread-pool-size", ModelType.INT)
            .setAllowExpression(true)
            .setRequired(false)
            .setDefaultValue(new ModelNode(16))
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    HotRodSessionManagementResourceDefinitionRegistrar() {
        super(SessionManagementResourceRegistration.HOTROD, CACHE_ATTRIBUTE_GROUP, HotRodSessionManagementProvider::new);
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder)
                .addAttributes(List.of(EXPIRATION_THREAD_POOL_SIZE))
                .requireSingletonChildResource(AffinityResourceRegistration.LOCAL)
                ;
    }
}
