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

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.jboss.as.clustering.controller.descriptions.SimpleResourceDescriptionResolver;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.clustering.jgroups.subsystem.ProtocolMetricsHandler.Attribute;
import org.jboss.as.clustering.jgroups.subsystem.ProtocolMetricsHandler.FieldType;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResourceBuilder;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.OverrideDescriptionProvider;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jgroups.Channel;
import org.jgroups.protocols.relay.RELAY2;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;
import org.wildfly.clustering.jgroups.spi.RelayConfiguration;
import org.wildfly.clustering.jgroups.spi.service.ChannelServiceName;

/**
 * @author Paul Ferraro
 */
public class ProtocolResourceRegistrationHandler implements OperationStepHandler, ProtocolMetricsHandler.ProtocolLocator {

    private final String name;
    private final PathAddress stackAddress;

    public ProtocolResourceRegistrationHandler(String name, PathAddress stackAddress) {
        this.name = name;
        this.stackAddress = stackAddress;
    }

    @Override
    public Protocol findProtocol(ServiceRegistry registry, PathAddress address) throws ClassNotFoundException, ModuleLoadException {
        String channelName = address.getElement(address.size() - 2).getValue();
        String protocolName = address.getElement(address.size() - 1).getValue();

        ServiceController<?> controller = registry.getService(ChannelServiceName.CHANNEL.getServiceName(channelName));
        if (controller != null) {
            Channel channel = (Channel) controller.getValue();
            if (channel != null) {
                controller = registry.getService(ChannelServiceName.FACTORY.getServiceName(channelName));
                ChannelFactory factory = (ChannelFactory) controller.getValue();
                if (factory != null) {
                    ProtocolStackConfiguration configuration = factory.getProtocolStackConfiguration();
                    for (ProtocolConfiguration protocol : configuration.getProtocols()) {
                        if (protocol.getName().equals(protocolName)) {
                            Class<? extends Protocol> protocolClass = configuration.getModuleLoader().loadModule(protocol.getModule()).getClassLoader().loadClass(protocol.getProtocolClassName()).asSubclass(Protocol.class);
                            return channel.getProtocolStack().findProtocol(protocolClass);
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {

        OverrideDescriptionProvider provider = new OverrideDescriptionProvider() {
            @Override
            public Map<String, ModelNode> getAttributeOverrideDescriptions(Locale locale) {
                return Collections.emptyMap();
            }

            @Override
            public Map<String, ModelNode> getChildTypeOverrideDescriptions(Locale locale) {
                StandardResourceDescriptionResolver resolver = JGroupsExtension.getResourceDescriptionResolver();
                String description = resolver.getChildTypeDescription(ProtocolResourceDefinition.WILDCARD_PATH.getKey(), locale, resolver.getResourceBundle(locale));
                ModelNode result = new ModelNode();
                result.get(ModelDescriptionConstants.DESCRIPTION).set(description);
                return Collections.singletonMap(ProtocolResourceDefinition.WILDCARD_PATH.getKey(), result);
            }
        };

        Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        ManagementResourceRegistration registration = context.getResourceRegistrationForUpdate().registerOverrideModel(this.name, provider);

        Resource stackResource = context.readResourceFromRoot(this.stackAddress, false);

        for (String name: stackResource.getChildrenNames(TransportResourceDefinition.WILDCARD_PATH.getKey())) {
            PathAddress transportAddress = this.stackAddress.append(TransportResourceDefinition.pathElement(name));
            ModelNode transport = context.readResourceFromRoot(transportAddress, false).getModel();
            ModuleIdentifier module = ModelNodes.asModuleIdentifier(ProtocolResourceDefinition.MODULE.resolveModelAttribute(context, transport));
            Class<? extends Protocol> transportClass = findProtocolClass(context, name, module);
            registration.registerSubModel(this.createProtocolResourceDefinition(name, transportClass)).setRuntimeOnly(true);
            resource.registerChild(ProtocolResourceDefinition.pathElement(name), PlaceholderResource.INSTANCE);
        }

        for (String name: stackResource.getChildrenNames(ProtocolResourceDefinition.WILDCARD_PATH.getKey())) {
            Resource protocolResource = context.readResourceFromRoot(this.stackAddress.append(ProtocolResourceDefinition.pathElement(name)), false);
            ModuleIdentifier module = ModelNodes.asModuleIdentifier(ProtocolResourceDefinition.MODULE.resolveModelAttribute(context, protocolResource.getModel()));
            Class<? extends Protocol> protocolClass = findProtocolClass(context, name, module);
            registration.registerSubModel(this.createProtocolResourceDefinition(name, protocolClass)).setRuntimeOnly(true);
            resource.registerChild(ProtocolResourceDefinition.pathElement(name), PlaceholderResource.INSTANCE);
        }

        if (stackResource.hasChild(RelayResourceDefinition.PATH)) {
            registration.registerSubModel(this.createProtocolResourceDefinition(RelayConfiguration.PROTOCOL_NAME, RELAY2.class)).setRuntimeOnly(true);
            resource.registerChild(ProtocolResourceDefinition.pathElement(RelayConfiguration.PROTOCOL_NAME), PlaceholderResource.INSTANCE);
        }
        context.stepCompleted();
    }

    private ResourceDefinition createProtocolResourceDefinition(String protocolName, Class<? extends Protocol> protocolClass) {

        SimpleResourceDescriptionResolver resolver = new SimpleResourceDescriptionResolver(protocolName, protocolClass.getSimpleName());
        ResourceBuilder builder = ResourceBuilder.Factory.create(ProtocolResourceDefinition.pathElement(protocolName), resolver);
        ProtocolMetricsHandler handler = new ProtocolMetricsHandler(this);

        for (Map.Entry<String, Attribute> entry: ProtocolMetricsHandler.findProtocolAttributes(protocolClass).entrySet()) {
            String name = entry.getKey();
            Attribute attribute = entry.getValue();
            FieldType type = FieldType.valueOf(attribute.getType());
            resolver.addDescription(name, attribute.getDescription());
            builder.addMetric(new SimpleAttributeDefinitionBuilder(name, type.getModelType()).setStorageRuntime().build(), handler);
        }

        return builder.build();
    }

    static Class<? extends Protocol> findProtocolClass(OperationContext context, String protocolName, ModuleIdentifier module) throws OperationFailedException {
        String className = protocolName;
        if (module.equals(ProtocolConfiguration.DEFAULT_MODULE) && !protocolName.startsWith(org.jgroups.conf.ProtocolConfiguration.protocol_prefix)) {
            className = org.jgroups.conf.ProtocolConfiguration.protocol_prefix + "." + protocolName;
        }

        try {
            ModuleLoader loader = (ModuleLoader) context.getServiceRegistry(false).getRequiredService(Services.JBOSS_SERVICE_MODULE_LOADER).getValue();
            return loader.loadModule(module).getClassLoader().loadClass(className).asSubclass(Protocol.class);
        } catch (ClassNotFoundException | ModuleLoadException e) {
            throw JGroupsLogger.ROOT_LOGGER.unableToLoadProtocolClass(className);
        }
    }
}
