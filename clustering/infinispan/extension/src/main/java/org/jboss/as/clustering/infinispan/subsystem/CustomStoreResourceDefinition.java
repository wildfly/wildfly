/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.infinispan.commons.util.AggregatedClassLoader;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.configuration.cache.StoreConfigurationBuilder;
import org.jboss.as.clustering.controller.SimpleResourceDescriptorConfigurator;
import org.jboss.as.clustering.infinispan.logging.InfinispanLogger;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.RequirementServiceBuilder;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.modules.Module;
import org.wildfly.clustering.server.util.MapEntry;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/cache=Y/store=STORE
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class CustomStoreResourceDefinition<C extends StoreConfiguration, B extends StoreConfigurationBuilder<C, B>> extends StoreResourceDefinition<C, B> {

    static final PathElement PATH = pathElement("custom");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
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
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    @SuppressWarnings("unchecked")
    CustomStoreResourceDefinition() {
        super(PATH, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH, WILDCARD_PATH), new SimpleResourceDescriptorConfigurator<>(Attribute.class), (Class<B>) (Class<?>) StoreConfigurationBuilder.class);
    }

    @Override
    public Map.Entry<Map.Entry<Supplier<B>, Consumer<B>>, Stream<Consumer<RequirementServiceBuilder<?>>>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        Map.Entry<Map.Entry<Supplier<B>, Consumer<B>>, Stream<Consumer<RequirementServiceBuilder<?>>>> entry = super.resolve(context, model);
        Consumer<B> configurator = entry.getKey().getValue();
        Stream<Consumer<RequirementServiceBuilder<?>>> dependencies = entry.getValue();

        PathAddress cacheAddress = context.getCurrentAddress().getParent();
        String containerName = cacheAddress.getParent().getLastElement().getValue();
        String cacheName = cacheAddress.getLastElement().getValue();

        String className = Attribute.CLASS.resolveModelAttribute(context, model).asString();

        ServiceDependency<List<Module>> cacheModules = ServiceDependency.on(CacheResourceDefinition.CACHE_MODULES, containerName, cacheName);
        Supplier<B> builderFactory = new Supplier<>() {
            @Override
            public B get() {
                List<Module> modules = cacheModules.get();
                ClassLoader loader = modules.size() > 1 ? new AggregatedClassLoader(modules.stream().map(Module::getClassLoader).collect(Collectors.toList())) : modules.get(0).getClassLoader();
                try {
                    @SuppressWarnings("unchecked")
                    Class<B> storeClass = (Class<B>) loader.loadClass(className).asSubclass(StoreConfigurationBuilder.class);
                    return new ConfigurationBuilder().persistence().addStore(storeClass);
                } catch (ClassNotFoundException | ClassCastException e) {
                    throw InfinispanLogger.ROOT_LOGGER.invalidCacheStore(e, className);
                }
            }
        };
        return MapEntry.of(MapEntry.of(builderFactory, configurator), Stream.concat(dependencies, Stream.of(cacheModules)));
    }
}
