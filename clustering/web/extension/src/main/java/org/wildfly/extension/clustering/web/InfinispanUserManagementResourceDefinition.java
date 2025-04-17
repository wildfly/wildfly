/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.infinispan.service.InfinispanCacheConfigurationAttributeGroup;
import org.wildfly.clustering.server.service.CacheConfigurationAttributeGroup;
import org.wildfly.common.function.Functions;
import org.wildfly.extension.clustering.web.sso.infinispan.InfinispanUserManagementProvider;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Definition of the /subsystem=distributable-web/infinispan-single-sign-on-management=* resource.
 * @author Paul Ferraro
 */
public class InfinispanUserManagementResourceDefinition extends UserManagementResourceDefinition {

    static final PathElement WILDCARD_PATH = PathElement.pathElement("infinispan-single-sign-on-management");
    static final CacheConfigurationAttributeGroup CACHE_ATTRIBUTE_GROUP = new InfinispanCacheConfigurationAttributeGroup(CAPABILITY);

    InfinispanUserManagementResourceDefinition() {
        super(WILDCARD_PATH, builder -> builder.addAttributes(CACHE_ATTRIBUTE_GROUP.getAttributes()));
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        return CapabilityServiceInstaller.builder(CAPABILITY, Functions.constantSupplier(new InfinispanUserManagementProvider(CACHE_ATTRIBUTE_GROUP.resolve(context, model)))).build();
    }
}
