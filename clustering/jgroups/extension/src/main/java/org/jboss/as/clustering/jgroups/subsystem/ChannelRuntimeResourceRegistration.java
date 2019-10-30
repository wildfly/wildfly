/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.clustering.jgroups.subsystem.AbstractProtocolResourceDefinition.Attribute.MODULE;
import static org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition.Attribute.STACK;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.jboss.as.clustering.controller.RuntimeResourceRegistration;
import org.jboss.as.clustering.controller.descriptions.SimpleResourceDescriptionResolver;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.clustering.jgroups.subsystem.ProtocolMetricsHandler.Attribute;
import org.jboss.as.clustering.jgroups.subsystem.ProtocolMetricsHandler.FieldType;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResourceBuilder;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.OverrideDescriptionProvider;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.ServiceRegistry;
import org.jgroups.JChannel;
import org.jgroups.protocols.TP;
import org.jgroups.protocols.relay.RELAY2;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;
import org.wildfly.clustering.jgroups.spi.RelayConfiguration;
import org.wildfly.clustering.service.PassiveServiceSupplier;

/**
 * @author Paul Ferraro
 */
public class ChannelRuntimeResourceRegistration implements RuntimeResourceRegistration, ProtocolMetricsHandler.ProtocolLocator {

    @Override
    public Protocol findProtocol(OperationContext context) throws ClassNotFoundException, ModuleLoadException {
        PathAddress address = context.getCurrentAddress();
        String channelName = address.getParent().getLastElement().getValue();
        String protocolName = address.getLastElement().getValue();

        ServiceRegistry registry = context.getServiceRegistry(true);
        JChannel channel = new PassiveServiceSupplier<JChannel>(registry, JGroupsRequirement.CHANNEL.getServiceName(context, channelName)).get();
        if (channel != null) {
            ChannelFactory factory = new PassiveServiceSupplier<ChannelFactory>(registry, JGroupsRequirement.CHANNEL_SOURCE.getServiceName(context, channelName)).get();
            if (factory != null) {
                ProtocolStackConfiguration configuration = factory.getProtocolStackConfiguration();
                ProtocolConfiguration<? extends TP> transport = configuration.getTransport();
                if (transport.getName().equals(protocolName)) {
                    Class<? extends Protocol> protocolClass = transport.createProtocol(configuration).getClass();
                    return channel.getProtocolStack().findProtocol(protocolClass);
                }
                for (ProtocolConfiguration<? extends Protocol> protocol : configuration.getProtocols()) {
                    if (protocol.getName().equals(protocolName)) {
                        Class<? extends Protocol> protocolClass = protocol.createProtocol(configuration).getClass();
                        return channel.getProtocolStack().findProtocol(protocolClass);
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void register(OperationContext context) throws OperationFailedException {
        OverrideDescriptionProvider provider = new OverrideDescriptionProvider() {
            @Override
            public Map<String, ModelNode> getAttributeOverrideDescriptions(Locale locale) {
                return Collections.emptyMap();
            }

            @Override
            public Map<String, ModelNode> getChildTypeOverrideDescriptions(Locale locale) {
                String description = JGroupsExtension.SUBSYSTEM_RESOLVER.getChildTypeDescription(ProtocolResourceDefinition.WILDCARD_PATH.getKey(), locale, JGroupsExtension.SUBSYSTEM_RESOLVER.getResourceBundle(locale));
                ModelNode result = new ModelNode();
                result.get(ModelDescriptionConstants.DESCRIPTION).set(description);
                return Collections.singletonMap(ProtocolResourceDefinition.WILDCARD_PATH.getKey(), result);
            }
        };

        Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        String stack = STACK.resolveModelAttribute(context, resource.getModel()).asString();
        ManagementResourceRegistration registration = context.getResourceRegistrationForUpdate().registerOverrideModel(context.getCurrentAddressValue(), provider);

        PathAddress stackAddress = context.getCurrentAddress().getParent().append(StackResourceDefinition.pathElement(stack));
        Resource stackResource = context.readResourceFromRoot(stackAddress, false);

        for (String name: stackResource.getChildrenNames(TransportResourceDefinition.WILDCARD_PATH.getKey())) {
            PathAddress transportAddress = stackAddress.append(TransportResourceDefinition.pathElement(name));
            ModelNode transport = context.readResourceFromRoot(transportAddress, false).getModel();
            String moduleName = MODULE.resolveModelAttribute(context, transport).asString();
            Class<? extends Protocol> transportClass = findProtocolClass(context, name, moduleName);
            registration.registerSubModel(this.createProtocolResourceDefinition(name, transportClass));
            resource.registerChild(ProtocolResourceDefinition.pathElement(name), PlaceholderResource.INSTANCE);
        }

        for (String name: stackResource.getChildrenNames(ProtocolResourceDefinition.WILDCARD_PATH.getKey())) {
            Resource protocolResource = context.readResourceFromRoot(stackAddress.append(ProtocolResourceDefinition.pathElement(name)), false);
            String moduleName = MODULE.resolveModelAttribute(context, protocolResource.getModel()).asString();
            Class<? extends Protocol> protocolClass = findProtocolClass(context, name, moduleName);
            registration.registerSubModel(this.createProtocolResourceDefinition(name, protocolClass));
            resource.registerChild(ProtocolResourceDefinition.pathElement(name), PlaceholderResource.INSTANCE);
        }

        if (stackResource.hasChild(RelayResourceDefinition.PATH)) {
            registration.registerSubModel(this.createProtocolResourceDefinition(RelayConfiguration.PROTOCOL_NAME, RELAY2.class));
            resource.registerChild(ProtocolResourceDefinition.pathElement(RelayConfiguration.PROTOCOL_NAME), PlaceholderResource.INSTANCE);
        }
    }

    @Override
    public void unregister(OperationContext context) {
        for (String name : context.readResource(PathAddress.EMPTY_ADDRESS).getChildrenNames(ProtocolResourceDefinition.WILDCARD_PATH.getKey())) {
            context.removeResource(PathAddress.pathAddress(ProtocolResourceDefinition.pathElement(name)));
        }
        context.getResourceRegistrationForUpdate().unregisterOverrideModel(context.getCurrentAddressValue());
    }

    private ResourceDefinition createProtocolResourceDefinition(String protocolName, Class<? extends Protocol> protocolClass) {

        SimpleResourceDescriptionResolver resolver = new SimpleResourceDescriptionResolver(protocolName, protocolClass.getSimpleName());
        ResourceBuilder builder = ResourceBuilder.Factory.create(ProtocolResourceDefinition.pathElement(protocolName), resolver).setRuntime();
        ProtocolMetricsHandler handler = new ProtocolMetricsHandler(this);

        for (Map.Entry<String, Attribute> entry: ProtocolMetricsHandler.findProtocolAttributes(protocolClass).entrySet()) {
            String name = entry.getKey();
            Attribute attribute = entry.getValue();
            FieldType type = FieldType.valueOf(attribute.getType());
            resolver.addDescription(name, attribute.getDescription());
            builder.addMetric(new SimpleAttributeDefinitionBuilder(name, type.getModelType(), true).setStorageRuntime().build(), handler);
        }

        return builder.build();
    }

    static Class<? extends Protocol> findProtocolClass(OperationContext context, String protocolName, String moduleName) throws OperationFailedException {
        String className = protocolName;
        if (moduleName.equals(AbstractProtocolResourceDefinition.Attribute.MODULE.getDefinition().getDefaultValue().asString()) && !protocolName.startsWith(org.jgroups.conf.ProtocolConfiguration.protocol_prefix)) {
            className = String.join(".", org.jgroups.conf.ProtocolConfiguration.protocol_prefix, protocolName);
        }

        try {
            return Module.getContextModuleLoader().loadModule(moduleName).getClassLoader().loadClass(className).asSubclass(Protocol.class);
        } catch (ClassNotFoundException | ModuleLoadException e) {
            throw JGroupsLogger.ROOT_LOGGER.unableToLoadProtocolClass(className);
        }
    }
}
