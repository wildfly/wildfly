/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;
import java.util.function.Function;
import java.util.stream.Stream;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.configuration.cache.StoreConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.jboss.as.clustering.infinispan.logging.InfinispanLogger;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.infinispan.service.InfinispanServiceDescriptor;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/cache=Y/store=STORE
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CustomStoreResourceDescription<C extends StoreConfiguration, B extends StoreConfigurationBuilder<C, B>> implements StoreResourceDescription<C, B> {

    enum Attribute implements AttributeDefinitionProvider {
        CLASS("class", ModelType.STRING)
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(true)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition get() {
            return this.definition;
        }
    }

    private final PathElement path = PersistenceResourceDescription.pathElement("custom");

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.concat(ResourceDescriptor.stream(EnumSet.allOf(Attribute.class)), StoreResourceDescription.super.getAttributes());
    }

    @Override
    public ServiceDependency<B> resolveStore(OperationContext context, ModelNode model) throws OperationFailedException {
        String className = Attribute.CLASS.resolveModelAttribute(context, model).asString();
        return ServiceDependency.on(InfinispanServiceDescriptor.CACHE_CONTAINER_CONFIGURATION, context.getCurrentAddress().getParent().getParent().getLastElement().getValue()).map(new Function<>() {
            @Override
            public B apply(GlobalConfiguration global) {
                try {
                    @SuppressWarnings("unchecked")
                    Class<B> storeClass = (Class<B>) global.classLoader().loadClass(className);
                    return new ConfigurationBuilder().persistence().addStore(storeClass);
                } catch (ClassNotFoundException | ClassCastException e) {
                    throw InfinispanLogger.ROOT_LOGGER.invalidCacheStore(e, className);
                }
            }
        });
    }
}
