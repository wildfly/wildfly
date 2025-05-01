/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;
import java.util.function.UnaryOperator;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfiguration;
import org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfigurationBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.AttributeDefinitionProvider;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for a file store.
 * @author Paul Ferraro
 */
public class FileStoreResourceDefinitionRegistrar extends StoreResourceDefinitionRegistrar<SoftIndexFileStoreConfiguration, SoftIndexFileStoreConfigurationBuilder> {

    enum DeprecatedAttribute implements AttributeDefinitionProvider, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        RELATIVE_PATH("path", ModelType.STRING, InfinispanSubsystemModel.VERSION_16_0_0) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(true);
            }
        },
        RELATIVE_TO("relative-to", ModelType.STRING, InfinispanSubsystemModel.VERSION_16_0_0) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setCapabilityReference(CapabilityReference.builder(CAPABILITY, PathManager.PATH_SERVICE_DESCRIPTOR).build());
            }
        },
        ;
        private final AttributeDefinition definition;

        DeprecatedAttribute(String name, ModelType type, InfinispanSubsystemModel deprecation) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type))
                    .setRequired(false)
                    .setDeprecated(deprecation.getVersion())
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition get() {
            return this.definition;
        }
    }

    FileStoreResourceDefinitionRegistrar() {
        super(new Configurator<>() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return StoreResourceRegistration.FILE;
            }

            @Override
            public ServiceDependency<SoftIndexFileStoreConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
                return ServiceDependency.from(ConfigurationBuilder::new).map(ConfigurationBuilder::persistence).map(PersistenceConfigurationBuilder::addSoftIndexFileStore);
            }
        });
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder).provideAttributes(EnumSet.allOf(DeprecatedAttribute.class));
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration = super.register(parent, context);

        PathManager pathManager = context.getPathManager().orElse(null);
        if (pathManager != null) {
            ResolvePathHandler pathHandler = ResolvePathHandler.Builder.of(pathManager)
                    .setPathAttribute(DeprecatedAttribute.RELATIVE_PATH.get())
                    .setRelativeToAttribute(DeprecatedAttribute.RELATIVE_TO.get())
                    .setDeprecated(DeprecatedAttribute.RELATIVE_TO.get().getDeprecationData().getSince())
                    .build();
            registration.registerOperationHandler(pathHandler.getOperationDefinition(), pathHandler);
        }

        return registration;
    }
}
