/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.infinispan.configuration.cache.AsyncStoreConfiguration;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.configuration.cache.StoreConfigurationBuilder;
import org.jboss.as.clustering.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.server.service.BinaryServiceConfiguration;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.ResourceModelResolver;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for the store of a cache.
 * @author Paul Ferraro
 */
public class StoreResourceDefinitionRegistrar<C extends StoreConfiguration, B extends StoreConfigurationBuilder<C, B>> extends PersistenceResourceDefinitionRegistrar {

    static final PropertiesAttributeDefinition PROPERTIES = new PropertiesAttributeDefinition.Builder().build();

    enum Attribute implements AttributeDefinitionProvider {
        MAX_BATCH_SIZE("max-batch-size", ModelType.INT, new ModelNode(100)),
        PASSIVATION("passivation", ModelType.BOOLEAN, ModelNode.FALSE),
        PRELOAD("preload", ModelType.BOOLEAN, ModelNode.FALSE),
        PURGE("purge", ModelType.BOOLEAN, ModelNode.FALSE),
        SHARED("shared", ModelType.BOOLEAN, ModelNode.FALSE),
        SEGMENTED("segmented", ModelType.BOOLEAN, ModelNode.TRUE),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition get() {
            return this.definition;
        }
    }

    enum DeprecatedAttribute implements AttributeDefinitionProvider {
        FETCH_STATE("fetch-state", ModelType.BOOLEAN, ModelNode.TRUE, InfinispanSubsystemModel.VERSION_16_0_0),
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(String name, ModelType type, ModelNode defaultValue, InfinispanSubsystemModel deprecation) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(false)
                    .setDefaultValue(defaultValue)
                    .setDeprecated(deprecation.getVersion())
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition get() {
            return this.definition;
        }
    }

    interface Configurator<C extends StoreConfiguration, B extends StoreConfigurationBuilder<C, B>> extends PersistenceResourceDefinitionRegistrar.Configurator, ResourceModelResolver<ServiceDependency<B>> {
    }

    private final ResourceModelResolver<ServiceDependency<B>> storeResolver;

    StoreResourceDefinitionRegistrar(Configurator<C, B> configurator) {
        super(configurator);
        this.storeResolver = configurator;
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder)
                .addAttributes(List.of(PROPERTIES))
                .provideAttributes(EnumSet.allOf(Attribute.class))
                .provideAttributes(EnumSet.allOf(DeprecatedAttribute.class))
                .requireSingletonChildResource(StoreWriteResourceRegistration.THROUGH)
                ;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration = super.register(parent, context);

        new StoreWriteBehindResourceDefinitionRegistrar().register(registration, context);
        new StoreWriteThroughResourceDefinitionRegistrar().register(registration, context);

        return registration;
    }

    @Override
    public ServiceDependency<PersistenceConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress storeAddress = context.getCurrentAddress();
        PathAddress cacheAddress = storeAddress.getParent();
        PathAddress containerAddress = cacheAddress.getParent();
        BinaryServiceConfiguration configuration = BinaryServiceConfiguration.of(containerAddress.getLastElement().getValue(), cacheAddress.getLastElement().getValue());
        ServiceDependency<AsyncStoreConfiguration> async = configuration.getServiceDependency(StoreWriteResourceDefinitionRegistrar.SERVICE_DESCRIPTOR);
        boolean passivation = Attribute.PASSIVATION.resolveModelAttribute(context, model).asBoolean();
        boolean preload = Attribute.PRELOAD.resolveModelAttribute(context, model).asBoolean();
        boolean purge = Attribute.PURGE.resolveModelAttribute(context, model).asBoolean();
        boolean segmented = Attribute.SEGMENTED.resolveModelAttribute(context, model).asBoolean();
        boolean shared = Attribute.SHARED.resolveModelAttribute(context, model).asBoolean();
        int maxBatchSize = Attribute.MAX_BATCH_SIZE.resolveModelAttribute(context, model).asInt();
        Map<String, String> properties = PROPERTIES.resolve(context, model);
        return this.storeResolver.resolve(context, model).combine(async, new BiFunction<>() {
            @Override
            public PersistenceConfigurationBuilder apply(B builder, AsyncStoreConfiguration async) {
                builder.maxBatchSize(maxBatchSize)
                    .preload(preload)
                    .purgeOnStartup(purge)
                    .segmented(segmented)
                    .shared(shared)
                    ;
                for (Map.Entry<String, String> entry : properties.entrySet()) {
                    builder.addProperty(entry.getKey(), entry.getValue());
                }
                builder.async().read(async);
                return builder.persistence().passivation(passivation);
            }
        });
    }
}
