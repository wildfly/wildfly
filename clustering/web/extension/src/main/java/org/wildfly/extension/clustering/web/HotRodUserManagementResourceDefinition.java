/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.infinispan.client.service.HotRodCacheConfigurationAttributeGroup;
import org.wildfly.clustering.server.service.CacheConfigurationAttributeGroup;
import org.wildfly.extension.clustering.web.sso.hotrod.HotRodUserManagementProvider;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class HotRodUserManagementResourceDefinition extends UserManagementResourceDefinition {

    static final PathElement WILDCARD_PATH = PathElement.pathElement("hotrod-single-sign-on-management");
    static final CacheConfigurationAttributeGroup CACHE_ATTRIBUTE_GROUP = new HotRodCacheConfigurationAttributeGroup(CAPABILITY);

    HotRodUserManagementResourceDefinition() {
        super(WILDCARD_PATH, builder -> builder.addAttributes(CACHE_ATTRIBUTE_GROUP.getAttributes()));
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        return CapabilityServiceInstaller.builder(CAPABILITY, new HotRodUserManagementProvider(CACHE_ATTRIBUTE_GROUP.resolve(context, model))).build();
    }
}
