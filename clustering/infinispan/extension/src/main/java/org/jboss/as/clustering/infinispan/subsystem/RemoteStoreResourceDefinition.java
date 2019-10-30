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

import java.util.List;

import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.SimpleResourceDescriptorConfigurator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource and its alias
 *
 * /subsystem=infinispan/cache-container=X/cache=Y/store=remote
 * /subsystem=infinispan/cache-container=X/cache=Y/remote-store=REMOTE_STORE
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 * @deprecated Use {@link org.jboss.as.clustering.infinispan.subsystem.remote.HotRodStoreResourceDefinition} instead.
 */
@Deprecated
public class RemoteStoreResourceDefinition extends StoreResourceDefinition {

    static final PathElement LEGACY_PATH = PathElement.pathElement("remote-store", "REMOTE_STORE");
    static final PathElement PATH = pathElement("remote");

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        CACHE("cache", ModelType.STRING, null),
        SOCKET_TIMEOUT("socket-timeout", ModelType.LONG, new ModelNode(60000L)),
        TCP_NO_DELAY("tcp-no-delay", ModelType.BOOLEAN, new ModelNode(true)),
        SOCKET_BINDINGS("remote-servers")
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setRequired(defaultValue == null)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setMeasurementUnit((type == ModelType.LONG) ? MeasurementUnit.MILLISECONDS : null)
                    .build();
        }

        Attribute(String name) {
            this.definition = new StringListAttributeDefinition.Builder(name)
                    .setCapabilityReference(new CapabilityReference(Capability.PERSISTENCE, CommonUnaryRequirement.OUTBOUND_SOCKET_BINDING))
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setMinSize(1)
                    .build();
        }

        @Override
        public AttributeDefinition getDefinition() {
            return this.definition;
        }
    }

    static void buildTransformation(ModelVersion version, ResourceTransformationDescriptionBuilder parent) {
        ResourceTransformationDescriptionBuilder builder = InfinispanModel.VERSION_4_0_0.requiresTransformation(version) ? parent.addChildRedirection(PATH, LEGACY_PATH) : parent.addChildResource(PATH);

        if (InfinispanModel.VERSION_4_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .setValueConverter(new AttributeConverter.DefaultAttributeConverter() {
                        @Override
                        protected void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
                            if (attributeValue.isDefined()) {
                                List<ModelNode> remoteServers = attributeValue.clone().asList();
                                ModelNode legacyListObject = new ModelNode();
                                for (ModelNode server : remoteServers) {
                                    ModelNode legacyListItem = new ModelNode();
                                    legacyListItem.get("outbound-socket-binding").set(server);
                                    legacyListObject.add(legacyListItem);
                                }
                                attributeValue.set(legacyListObject);
                            }
                        }
                    }, Attribute.SOCKET_BINDINGS.getDefinition());
        }

        StoreResourceDefinition.buildTransformation(version, builder, PATH);
    }

    RemoteStoreResourceDefinition() {
        super(PATH, LEGACY_PATH, InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(PATH, WILDCARD_PATH), new SimpleResourceDescriptorConfigurator<>(Attribute.class), RemoteStoreServiceConfigurator::new);
        this.setDeprecated(InfinispanModel.VERSION_7_0_0.getVersion());
    }
}
