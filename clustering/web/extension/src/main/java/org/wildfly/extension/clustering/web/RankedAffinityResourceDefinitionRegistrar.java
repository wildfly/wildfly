/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.clustering.web;

import java.util.EnumSet;
import java.util.function.Supplier;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.session.cache.affinity.NarySessionAffinityConfiguration;
import org.wildfly.clustering.web.service.routing.RouteLocatorProvider;
import org.wildfly.clustering.web.service.routing.RoutingProvider;
import org.wildfly.extension.clustering.web.deployment.MutableRankedRoutingConfiguration;
import org.wildfly.extension.clustering.web.routing.infinispan.RankedRouteLocatorProvider;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ResourceDescriptor;

/**
 * Registers a resource definition for a ranked affinity resource.
 * @author Paul Ferraro
 */
public class RankedAffinityResourceDefinitionRegistrar extends AffinityResourceDefinitionRegistrar {

    enum Attribute implements AttributeDefinitionProvider {
        DELIMITER("delimiter", ModelType.STRING, new ModelNode(MutableRankedRoutingConfiguration.DEFAULT_DELIMITER)),
        MAX_ROUTES("max-routes", ModelType.STRING, new ModelNode(MutableRankedRoutingConfiguration.DEFAULT_MAX_MEMBERS)),
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
        public AttributeDefinition get() {
            return this.definition;
        }
    }

    RankedAffinityResourceDefinitionRegistrar() {
        super(AffinityResourceRegistration.RANKED, RoutingProvider.INFINISPAN_SERVICE_DESCRIPTOR);
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder).provideAttributes(EnumSet.allOf(Attribute.class));
    }

    @Override
    public Supplier<RouteLocatorProvider> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        String delimiter = Attribute.DELIMITER.resolveModelAttribute(context, model).asString();
        int maxRoutes = Attribute.MAX_ROUTES.resolveModelAttribute(context, model).asInt();
        return new Supplier<>() {
            @Override
            public RouteLocatorProvider get() {
                return new RankedRouteLocatorProvider(new NarySessionAffinityConfiguration() {
                    @Override
                    public int getMaxMembers() {
                        return maxRoutes;
                    }

                    @Override
                    public String getDelimiter() {
                        return delimiter;
                    }
                });
            }
        };
    }
}
