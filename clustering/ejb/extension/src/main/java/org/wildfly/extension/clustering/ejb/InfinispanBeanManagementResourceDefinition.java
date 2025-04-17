/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.ejb;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.ejb.bean.BeanManagementProvider;
import org.wildfly.clustering.ejb.infinispan.bean.InfinispanBeanManagementProvider;
import org.wildfly.clustering.infinispan.service.InfinispanCacheConfigurationAttributeGroup;
import org.wildfly.clustering.server.service.CacheConfigurationAttributeGroup;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Definition of the /subsystem=distributable-ejb/infinispan-bean-management=* resource.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class InfinispanBeanManagementResourceDefinition extends BeanManagementResourceDefinition {

    static PathElement pathElement(String name) {
        return PathElement.pathElement("infinispan-bean-management", name);
    }
    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);
    static final CacheConfigurationAttributeGroup CACHE_ATTRIBUTE_GROUP = new InfinispanCacheConfigurationAttributeGroup(BEAN_MANAGEMENT_PROVIDER);

    InfinispanBeanManagementResourceDefinition() {
        super(WILDCARD_PATH, builder -> builder.addAttributes(CACHE_ATTRIBUTE_GROUP.getAttributes()));
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();
        BeanManagementProvider provider = new InfinispanBeanManagementProvider<>(name, this.resolve(context, model), CACHE_ATTRIBUTE_GROUP.resolve(context, model));
        return CapabilityServiceInstaller.builder(BEAN_MANAGEMENT_PROVIDER, provider).build();
    }
}
