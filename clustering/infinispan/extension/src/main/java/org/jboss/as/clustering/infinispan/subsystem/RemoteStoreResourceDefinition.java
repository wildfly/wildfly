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

import org.infinispan.commons.api.BasicCacheContainer;
import org.jboss.as.clustering.controller.AddStepHandler;
import org.jboss.as.clustering.controller.AttributeParsers;
import org.jboss.as.clustering.controller.CapabilityReference;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.RemoveStepHandler;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleAliasEntry;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.controller.transform.LegacyPropertyAddOperationTransformer;
import org.jboss.as.clustering.controller.transform.LegacyPropertyResourceTransformer;
import org.jboss.as.clustering.controller.transform.SimpleOperationTransformer;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
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
 */
public class RemoteStoreResourceDefinition extends StoreResourceDefinition {

    static final PathElement LEGACY_PATH = PathElement.pathElement("remote-store", "REMOTE_STORE");
    static final PathElement PATH = pathElement("remote");

    enum Capability implements org.jboss.as.clustering.controller.Capability {
        OUTBOUND_SOCKET_BINDING("org.wildfly.clustering.infinispan.cache-container.cache.store.remote.outbound-socket-binding"),
        ;
        private final RuntimeCapability<Void> definition;

        Capability(String name) {
            this.definition = RuntimeCapability.Builder.of(name, true).build();
        }

        @Override
        public RuntimeCapability<Void> getDefinition() {
            return this.definition;
        }

        @Override
        public RuntimeCapability<Void> resolve(PathAddress address) {
            PathAddress cacheAddress = address.getParent();
            PathAddress containerAddress = cacheAddress.getParent();
            return this.definition.fromBaseCapability(containerAddress.getLastElement().getValue() + "." + cacheAddress.getLastElement().getValue());
        }
    }

    enum Attribute implements org.jboss.as.clustering.controller.Attribute {
        CACHE("cache", ModelType.STRING, new ModelNode(BasicCacheContainer.DEFAULT_CACHE_NAME)),
        SOCKET_TIMEOUT("socket-timeout", ModelType.LONG, new ModelNode(60000L)),
        TCP_NO_DELAY("tcp-no-delay", ModelType.BOOLEAN, new ModelNode(true)),
        SOCKET_BINDINGS("remote-servers")
        ;
        private final AttributeDefinition definition;

        Attribute(String name, ModelType type, ModelNode defaultValue) {
            this.definition = new SimpleAttributeDefinitionBuilder(name, type)
                    .setAllowExpression(true)
                    .setAllowNull(true)
                    .setDefaultValue(defaultValue)
                    .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                    .setMeasurementUnit((type == ModelType.LONG) ? MeasurementUnit.MILLISECONDS : null)
                    .build();
        }

        Attribute(String name) {
            this.definition = new StringListAttributeDefinition.Builder(name)
                    .setAttributeParser(AttributeParsers.COLLECTION)
                    .setCapabilityReference(new CapabilityReference(Capability.OUTBOUND_SOCKET_BINDING, CommonUnaryRequirement.OUTBOUND_SOCKET_BINDING))
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

        if (InfinispanModel.VERSION_3_0_0.requiresTransformation(version)) {
            builder.addOperationTransformationOverride(ModelDescriptionConstants.ADD)
                    .setCustomOperationTransformer(new SimpleOperationTransformer(new LegacyPropertyAddOperationTransformer())).inheritResourceAttributeDefinitions();

            builder.setCustomResourceTransformer(new LegacyPropertyResourceTransformer());
        }

        StoreResourceDefinition.buildTransformation(version, builder);
    }

    RemoteStoreResourceDefinition(boolean allowRuntimeOnlyRegistration) {
        super(PATH, new InfinispanResourceDescriptionResolver(PATH, WILDCARD_PATH), allowRuntimeOnlyRegistration);
    }

    @Override
    public void register(ManagementResourceRegistration parentRegistration) {
        ManagementResourceRegistration registration = parentRegistration.registerSubModel(this);
        parentRegistration.registerAlias(LEGACY_PATH, new SimpleAliasEntry(registration));

        ResourceDescriptor descriptor = new ResourceDescriptor(this.getResourceDescriptionResolver())
                .addAttributes(Attribute.class)
                .addAttributes(StoreResourceDefinition.Attribute.class)
                .addCapabilities(Capability.class)
                .addRequiredSingletonChildren(StoreWriteThroughResourceDefinition.PATH)
                ;
        ResourceServiceHandler handler = new SimpleResourceServiceHandler<>(address -> new RemoteStoreBuilder(address.getParent()));
        new AddStepHandler(descriptor, handler).register(registration);
        new RemoveStepHandler(descriptor, handler).register(registration);

        super.register(registration);
    }
}
