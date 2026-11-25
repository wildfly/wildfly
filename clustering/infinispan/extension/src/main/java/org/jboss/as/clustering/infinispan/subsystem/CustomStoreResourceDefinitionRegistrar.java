/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.List;
import java.util.function.Function;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.configuration.cache.StoreConfigurationBuilder;
import org.jboss.as.clustering.infinispan.logging.InfinispanLogger;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a custom store resource definition.
 * @author Paul Ferraro
 */
public class CustomStoreResourceDefinitionRegistrar<C extends StoreConfiguration, B extends StoreConfigurationBuilder<C, B>> extends StoreResourceDefinitionRegistrar<C, B> {

    static final AttributeDefinition CLASS = new SimpleAttributeDefinitionBuilder("class", ModelType.STRING)
            .setAllowExpression(true)
            .setRequired(true)
            .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
            .build();

    CustomStoreResourceDefinitionRegistrar() {
        super(new Configurator<>() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return StoreResourceRegistration.CUSTOM;
            }

            @Override
            public ServiceDependency<B> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
                PathAddress cacheAddress = context.getCurrentAddress().getParent();
                PathAddress containerAddress = cacheAddress.getParent();
                BinaryServiceConfiguration configuration = BinaryServiceConfiguration.of(containerAddress.getLastElement().getValue(), cacheAddress.getLastElement().getValue());
                String className = CLASS.resolveModelAttribute(context, model).asString();
                return configuration.getServiceDependency(CacheResourceDefinitionRegistrar.CLASS_LOADER).map(new Function<>() {
                    @Override
                    public B apply(ClassLoader loader) {
                        try {
                            @SuppressWarnings("unchecked")
                            Class<B> storeClass = (Class<B>) loader.loadClass(className);
                            return new ConfigurationBuilder().persistence().addStore(storeClass);
                        } catch (ClassNotFoundException | ClassCastException e) {
                            throw InfinispanLogger.ROOT_LOGGER.invalidCacheStore(e, className);
                        }
                    }
                });
            }
        });
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder).addAttributes(List.of(CLASS));
    }
}
