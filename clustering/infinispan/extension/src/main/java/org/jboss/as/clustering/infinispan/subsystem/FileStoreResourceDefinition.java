/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.UnaryOperator;

import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.ManagementResourceRegistration;
import org.jboss.as.clustering.controller.SimpleResourceDescriptorConfigurator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/cache=Y/store=STORE
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class FileStoreResourceDefinition extends StoreResourceDefinition {

    static final PathElement LEGACY_PATH = PathElement.pathElement("file-store", "FILE_STORE");
    static final PathElement PATH = pathElement("file");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute, UnaryOperator<SimpleAttributeDefinitionBuilder> {
        RELATIVE_PATH("path", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setAllowExpression(true);
            }
        },
        RELATIVE_TO("relative-to", ModelType.STRING) {
            @Override
            public SimpleAttributeDefinitionBuilder apply(SimpleAttributeDefinitionBuilder builder) {
                return builder.setDefaultValue(new ModelNode(ServerEnvironment.SERVER_DATA_DIR))
                        .setCapabilityReference(new CapabilityReference(Capability.PERSISTENCE, CommonUnaryRequirement.PATH))
                        ;
            }
        },
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = this.apply(new SimpleAttributeDefinitionBuilder(name, type).setRequired(false).setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)).build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = InfinispanModel.VERSION_4_0_0.requiresTransformation(version) ? parent.addChildRedirection(PATH, LEGACY_PATH) : parent.addChildResource(PATH);

        if (InfinispanModel.VERSION_5_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, Attribute.RELATIVE_TO.getName());
        }

        StoreResourceDefinition.buildTransformation(version, builder, PATH);
    }

    FileStoreResourceDefinition() {
        super(PATH, LEGACY_PATH, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH, WILDCARD_PATH), new SimpleResourceDescriptorConfigurator<>(Attribute.class), FileStoreServiceConfigurator::new);
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent) {
        ManagementResourceRegistration registration = super.register(parent);

        PathManager pathManager = registration.getPathManager().orElse(null);
        if (pathManager != null) {
            ResolvePathHandler pathHandler = ResolvePathHandler.Builder.of(pathManager)
                    .setPathAttribute(Attribute.RELATIVE_PATH.getDefinition())
                    .setRelativeToAttribute(Attribute.RELATIVE_TO.getDefinition())
                    .build();
            registration.registerOperationHandler(pathHandler.getOperationDefinition(), pathHandler);
        }

        return registration;
    }
}
