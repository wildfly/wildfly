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
package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.clustering.controller.ParentResourceServiceHandler;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.RestartParentResourceAddStepHandler;
import org.jboss.as.clustering.controller.RestartParentResourceRemoveStepHandler;
import org.jboss.as.clustering.controller.SimpleAliasEntry;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelType;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.RelayConfiguration;

/**
 * Resource definition for /subsystem=jgroups/stack=X/relay=RELAY
 *
 * @author Paul Ferraro
 */
public class RelayResourceDefinition extends ProtocolResourceDefinition {

    static final PathElement PATH = pathElement(RelayConfiguration.PROTOCOL_NAME);
    static final PathElement LEGACY_PATH = pathElement("RELAY");
    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    public static PathElement pathElement(String name) {
        return PathElement.pathElement("relay", name);
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        SITE("site", ModelType.STRING),
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setAllowNull(false)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    @SuppressWarnings("deprecation")
    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = parent.addChildResource(PATH);

        RemoteSiteResourceDefinition.buildTransformation(version, builder);
        PropertyResourceDefinition.buildTransformation(version, builder);
    }

    private final ResourceServiceBuilderFactory<RelayConfiguration> builderFactory = address -> new RelayConfigurationBuilder(address);

    RelayResourceDefinition(ResourceServiceBuilderFactory<ChannelFactory> parentBuilderFactory) {
        super(new Parameters(PATH, new JGroupsResourceDescriptionResolver(WILDCARD_PATH, ProtocolResourceDefinition.WILDCARD_PATH)), parentBuilderFactory);
    }

    @Override
    public void register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this);
        parentRegistration.registerAlias(LEGACY_PATH, new SimpleAliasEntry(registration));

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addAttributes(ProtocolResourceDefinition.Attribute.PROPERTIES)
                ;
        ResourceServiceHandler handler = new ParentResourceServiceHandler<>(this.builderFactory);
        new RestartParentResourceAddStepHandler<>(this.parentBuilderFactory, descriptor, handler).register(registration);
        new RestartParentResourceRemoveStepHandler<>(this.parentBuilderFactory, descriptor, handler).register(registration);

        new RemoteSiteResourceDefinition(this.builderFactory).register(registration);

        super.register(registration);
    }
}
