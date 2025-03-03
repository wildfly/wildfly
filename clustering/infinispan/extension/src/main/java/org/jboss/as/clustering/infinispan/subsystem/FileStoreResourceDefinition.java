/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.UnaryOperator;

import org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfiguration;
import org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfigurationBuilder;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.SimpleResourceDescriptorConfigurator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.capability.CapabilityReference;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/cache=Y/store=STORE
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class FileStoreResourceDefinition extends StoreResourceDefinition<SoftIndexFileStoreConfiguration, SoftIndexFileStoreConfigurationBuilder> {

    static final PathElement PATH = pathElement("file");

    enum DeprecatedAttribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
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
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type).setRequired(false).setDeprecated(deprecation.getVersion()).setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    FileStoreResourceDefinition() {
        super(PATH, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH, WILDCARD_PATH), new SimpleResourceDescriptorConfigurator<>(DeprecatedAttribute.class), SoftIndexFileStoreConfigurationBuilder.class);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);

        PathManager pathManager = registration.getPathManager().orElse(null);
        if (pathManager != null) {
            ResolvePathHandler pathHandler = ResolvePathHandler.Builder.of(pathManager)
                    .setPathAttribute(DeprecatedAttribute.RELATIVE_PATH.getDefinition())
                    .setRelativeToAttribute(DeprecatedAttribute.RELATIVE_TO.getDefinition())
                    .setDeprecated(DeprecatedAttribute.RELATIVE_TO.getDefinition().getDeprecationData().getSince())
                    .build();
            registration.registerOperationHandler(pathHandler.getOperationDefinition(), pathHandler);
        }

        return registration;
    }
}
