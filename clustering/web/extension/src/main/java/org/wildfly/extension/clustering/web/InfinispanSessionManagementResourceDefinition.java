/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.infinispan.service.InfinispanCacheConfigurationAttributeGroup;
import org.wildfly.clustering.server.service.CacheConfigurationAttributeGroup;
import org.wildfly.clustering.web.service.routing.RouteLocatorProvider;
import org.wildfly.extension.clustering.web.session.infinispan.InfinispanSessionManagementProvider;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Definition of the /subsystem=distributable-web/infinispan-session-management=* resource.
 * @author Paul Ferraro
 */
public class InfinispanSessionManagementResourceDefinition extends SessionManagementResourceDefinition {

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);
    static PathElement pathElement(String name) {
        return PathElement.pathElement("infinispan-session-management", name);
    }
    static final CacheConfigurationAttributeGroup CACHE_ATTRIBUTE_GROUP = new InfinispanCacheConfigurationAttributeGroup(SESSION_MANAGEMENT_PROVIDER);

    InfinispanSessionManagementResourceDefinition() {
        super(WILDCARD_PATH, new UnaryOperator<>() {
            @Override
            public ResourceDescriptor apply(ResourceDescriptor descriptor) {
                return descriptor.addAttributes(CACHE_ATTRIBUTE_GROUP.getAttributes())
                        .addRequiredSingletonChildren(PrimaryOwnerAffinityResourceDefinition.PATH)
                        ;
            }
        });
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);

        new PrimaryOwnerAffinityResourceDefinition().register(registration);
        new RankedAffinityResourceDefinition().register(registration);

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        ServiceDependency<RouteLocatorProvider> locatorProvider = ServiceDependency.on(RouteLocatorProvider.SERVICE_DESCRIPTOR, context.getCurrentAddressValue());
        return CapabilityServiceInstaller.builder(SessionManagementResourceDefinition.SESSION_MANAGEMENT_PROVIDER, new InfinispanSessionManagementProvider(this.resolve(context, model), CACHE_ATTRIBUTE_GROUP.resolve(context, model), locatorProvider))
                .requires(locatorProvider)
                .build();
    }
}
