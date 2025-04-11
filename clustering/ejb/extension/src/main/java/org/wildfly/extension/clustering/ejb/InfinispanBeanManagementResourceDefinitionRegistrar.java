/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.ejb.bean.BeanManagementProvider;
import org.wildfly.clustering.ejb.infinispan.bean.InfinispanBeanManagementProvider;
import org.wildfly.clustering.infinispan.service.InfinispanCacheConfigurationAttributeGroup;
import org.wildfly.clustering.server.service.CacheConfigurationAttributeGroup;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Registers a resource definition for an embedded Infinispan bean management provider.
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class InfinispanBeanManagementResourceDefinitionRegistrar extends BeanManagementResourceDefinitionRegistrar {

    static final CacheConfigurationAttributeGroup CACHE_ATTRIBUTE_GROUP = new InfinispanCacheConfigurationAttributeGroup(CAPABILITY);

    InfinispanBeanManagementResourceDefinitionRegistrar() {
        super(BeanManagementResourceRegistration.INFINISPAN);
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder).addAttributes(CACHE_ATTRIBUTE_GROUP.getAttributes());
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();
        BeanManagementProvider provider = new InfinispanBeanManagementProvider<>(name, this.resolve(context, model), CACHE_ATTRIBUTE_GROUP.resolve(context, model));
        return CapabilityServiceInstaller.builder(CAPABILITY, provider).build();
    }
}
