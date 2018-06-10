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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.jboss.as.clustering.controller.RuntimeResourceRegistration;
import org.jboss.as.clustering.jgroups.subsystem.ProtocolMetricsHandler.Attribute;
import org.jboss.as.clustering.jgroups.subsystem.ProtocolMetricsHandler.FieldType;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.OverrideDescriptionProvider;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.ServiceRegistry;
import org.jgroups.JChannel;
import org.jgroups.protocols.FORK;
import org.jgroups.protocols.TP;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;
import org.wildfly.clustering.service.PassiveServiceSupplier;

/**
 * Operation handler for registration of fork protocol runtime resources.
 * @author Paul Ferraro
 */
public class ForkProtocolRuntimeResourceRegistration implements RuntimeResourceRegistration, ProtocolMetricsHandler.ProtocolLocator {

    @Override
    public Protocol findProtocol(OperationContext context) throws ClassNotFoundException, ModuleLoadException {
        PathAddress address = context.getCurrentAddress();
        String channelName = address.getElement(address.size() - 3).getValue();
        String forkName = address.getElement(address.size() - 2).getValue();
        String protocolName = address.getElement(address.size() - 1).getValue();

        ServiceRegistry registry = context.getServiceRegistry(true);
        JChannel channel = new PassiveServiceSupplier<JChannel>(registry, JGroupsRequirement.CHANNEL.getServiceName(context, channelName)).get();
        if (channel != null) {
            FORK fork = (FORK) channel.getProtocolStack().findProtocol(FORK.class);
            if (fork != null) {
                ChannelFactory factory = new PassiveServiceSupplier<ChannelFactory>(registry, JGroupsRequirement.CHANNEL_FACTORY.getServiceName(context, channelName)).get();
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
                            return fork.get(forkName).getProtocolStack().findProtocol(protocolClass);
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void register(OperationContext context) throws OperationFailedException {

        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        ManagementResourceRegistration registration = context.getResourceRegistrationForUpdate();
        String protocolName = context.getCurrentAddressValue();
        String moduleName = ProtocolResourceDefinition.Attribute.MODULE.resolveModelAttribute(context, resource.getModel()).asString();
        Class<? extends Protocol> protocolClass = ChannelRuntimeResourceRegistration.findProtocolClass(context, protocolName, moduleName);

        Map<String, Attribute> attributes = ProtocolMetricsHandler.findProtocolAttributes(protocolClass);

        // If this is a wildcard registration, create an override model registration with which to register protocol-specific metrics
        if (registration.getPathAddress().getLastElement().isWildcard()) {
            OverrideDescriptionProvider provider = new OverrideDescriptionProvider() {
                @Override
                public Map<String, ModelNode> getAttributeOverrideDescriptions(Locale locale) {
                    Map<String, ModelNode> result = new HashMap<>();
                    for (Attribute attribute : attributes.values()) {
                        ModelNode value = new ModelNode();
                        value.get(ModelDescriptionConstants.DESCRIPTION).set(attribute.getDescription());
                        result.put(attribute.getName(), value);
                    }
                    return result;
                }

                @Override
                public Map<String, ModelNode> getChildTypeOverrideDescriptions(Locale locale) {
                    return Collections.emptyMap();
                }
            };
            registration = registration.registerOverrideModel(protocolName, provider);
        }

        ProtocolMetricsHandler handler = new ProtocolMetricsHandler(this);

        for (Attribute attribute : attributes.values()) {
            String name = attribute.getName();
            FieldType type = FieldType.valueOf(attribute.getType());
            registration.registerMetric(new SimpleAttributeDefinitionBuilder(name, type.getModelType()).setStorageRuntime().build(), handler);
        }
    }

    @Override
    public void unregister(OperationContext context) throws OperationFailedException {
        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        ManagementResourceRegistration registration = context.getResourceRegistrationForUpdate();
        String protocolName = context.getCurrentAddressValue();
        String moduleName = ProtocolResourceDefinition.Attribute.MODULE.resolveModelAttribute(context, resource.getModel()).asString();
        Class<? extends Protocol> protocolClass = ChannelRuntimeResourceRegistration.findProtocolClass(context, protocolName, moduleName);

        for (String attribute : ProtocolMetricsHandler.findProtocolAttributes(protocolClass).keySet()) {
            registration.unregisterAttribute(attribute);
        }
    }
}
