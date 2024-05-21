/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import static org.wildfly.extension.clustering.web.RankedAffinityResourceDefinition.Attribute.DELIMITER;
import static org.wildfly.extension.clustering.web.RankedAffinityResourceDefinition.Attribute.MAX_ROUTES;

import org.jboss.as.clustering.controller.SimpleResourceDescriptorConfigurator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.session.cache.affinity.NarySessionAffinityConfiguration;
import org.wildfly.clustering.web.service.routing.RouteLocatorProvider;
import org.wildfly.clustering.web.service.routing.RoutingProvider;
import org.wildfly.common.function.Functions;
import org.wildfly.extension.clustering.web.routing.infinispan.RankedRouteLocatorProvider;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class RankedAffinityResourceDefinition extends AffinityResourceDefinition {

    static final PathElement PATH = pathElement("ranked");

    static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(RouteLocatorProvider.SERVICE_DESCRIPTOR)
            .setAllowMultipleRegistrations(true)
            .setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT)
            .addRequirements(RoutingProvider.INFINISPAN_SERVICE_DESCRIPTOR.getName())
            .build();

    public enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        DELIMITER("delimiter", ModelType.STRING, new ModelNode(".")),
        MAX_ROUTES("max-routes", ModelType.STRING, new ModelNode(3)),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(false)
                    .setDefaultValue(defaultValue)
                    .setRequired(false)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    RankedAffinityResourceDefinition() {
        super(PATH, CAPABILITY, new SimpleResourceDescriptorConfigurator<>(Attribute.class));
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String delimiter = DELIMITER.resolveModelAttribute(context, model).asString();
        int maxRoutes = MAX_ROUTES.resolveModelAttribute(context, model).asInt();
        NarySessionAffinityConfiguration configuration = new NarySessionAffinityConfiguration() {
            @Override
            public int getMaxMembers() {
                return maxRoutes;
            }

            @Override
            public String getDelimiter() {
                return delimiter;
            }
        };
        return CapabilityServiceInstaller.builder(CAPABILITY, RankedRouteLocatorProvider::new, Functions.constantSupplier(configuration)).build();
    }
}
