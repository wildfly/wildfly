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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.msc.service.ServiceController.Mode.ON_DEMAND;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;

import org.jboss.as.clustering.controller.descriptions.SimpleResourceDescriptionResolver;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.clustering.jgroups.subsystem.ProtocolMetricsHandler.Attribute;
import org.jboss.as.clustering.jgroups.subsystem.ProtocolMetricsHandler.FieldType;
import org.jboss.as.clustering.msc.InjectedValueServiceBuilder;
import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResourceBuilder;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.descriptions.OverrideDescriptionProvider;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.Resource.ResourceEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jgroups.Channel;
import org.jgroups.conf.ProtocolConfiguration;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.spi.ClusteredGroupServiceInstaller;
import org.wildfly.clustering.spi.GroupServiceInstaller;

/**
 * Handler for /subsystem=jgroups/channel=*:add() operations
 * @author Paul Ferraro
 */
public class ChannelAddHandler extends AbstractAddStepHandler {

    private final boolean allowRuntimeOnlyRegistration;

    ChannelAddHandler(boolean allowRuntimeOnlyRegistration, AttributeDefinition... attributes) {
        super(attributes);
        this.allowRuntimeOnlyRegistration = allowRuntimeOnlyRegistration;
    }

    @Override
    protected void populateModel(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {

        super.populateModel(context, operation, resource);

        final PathAddress channelAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String channelName = channelAddress.getLastElement().getValue();
        final String stackName = ModelNodes.asString(ChannelResourceDefinition.STACK.resolveModelAttribute(context, resource.getModel()));

        if (this.allowRuntimeOnlyRegistration && (context.getRunningMode() == RunningMode.NORMAL)) {
            OperationStepHandler handler = new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    PathAddress subsystemAddress = channelAddress.subAddress(0, channelAddress.size() - 1);
                    // Lookup the name of the default stack if necessary
                    PathAddress stackAddress = subsystemAddress.append(StackResourceDefinition.pathElement((stackName != null) ? stackName : JGroupsSubsystemResourceDefinition.DEFAULT_STACK.resolveModelAttribute(context, context.readResourceFromRoot(subsystemAddress).getModel()).asString()));

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
                            result.get(DESCRIPTION).set(description);
                            return Collections.singletonMap(ProtocolResourceDefinition.WILDCARD_PATH.getKey(), result);
                        }
                    };

                    Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
                    ManagementResourceRegistration registration = context.getResourceRegistrationForUpdate().registerOverrideModel(channelName, provider);

                    PathAddress transportAddress = stackAddress.append(TransportResourceDefinition.PATH);
                    ModelNode transport = context.readResourceFromRoot(transportAddress).getModel();
                    String transportName = ProtocolResourceDefinition.TYPE.resolveModelAttribute(context, transport).asString();
                    registration.registerSubModel(createProtocolResourceDefinition(transportName)).setRuntimeOnly(true);
                    resource.registerChild(ProtocolResourceDefinition.pathElement(transportName), PlaceholderResource.INSTANCE);

                    Resource stackResource = context.readResourceFromRoot(stackAddress);
                    for (ResourceEntry entry: stackResource.getChildren(ProtocolResourceDefinition.WILDCARD_PATH.getKey())) {
                        String protocolName = entry.getName();
                        registration.registerSubModel(createProtocolResourceDefinition(protocolName)).setRuntimeOnly(true);
                        resource.registerChild(ProtocolResourceDefinition.pathElement(protocolName), PlaceholderResource.INSTANCE);
                    }

                    if (stackResource.hasChild(RelayResourceDefinition.PATH)) {
                        String relayName = "relay.RELAY2";
                        registration.registerSubModel(createProtocolResourceDefinition(relayName)).setRuntimeOnly(true);
                        resource.registerChild(ProtocolResourceDefinition.pathElement(relayName), PlaceholderResource.INSTANCE);
                    }
                    context.stepCompleted();
                }
            };
            context.addStep(handler, OperationContext.Stage.MODEL);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        installRuntimeServices(context, operation, model);
    }

    static void installRuntimeServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        String channelName = address.getLastElement().getValue();
        String stackName = ModelNodes.asString(ChannelResourceDefinition.STACK.resolveModelAttribute(context, model), ChannelFactoryService.DEFAULT);

        ModuleIdentifier module = ModelNodes.asModuleIdentifier(ChannelResourceDefinition.MODULE.resolveModelAttribute(context, model));

        ServiceTarget target = context.getServiceTarget();

        // Install channel factory alias
        new InjectedValueServiceBuilder(target).build(ChannelService.getFactoryServiceName(channelName), ChannelFactoryService.getServiceName(stackName), ChannelFactory.class).install();

        // Install channel
        ChannelService.build(target, channelName).setInitialMode(ON_DEMAND).install();

        // Install channel connector
        ConnectedChannelService.build(target, channelName).setInitialMode(ON_DEMAND).install();

        // Install channel jndi binding
        new BinderServiceBuilder(target).build(ChannelService.createChannelBinding(channelName), ChannelService.getServiceName(channelName), Channel.class).install();

        // Install fork channel factory
        ForkChannelFactoryService.build(target, channelName).setInitialMode(ON_DEMAND).install();

        // Install fork channel factory jndi binding
        new BinderServiceBuilder(target).build(ChannelFactoryService.createChannelFactoryBinding(channelName), ChannelFactoryService.getServiceName(channelName), ChannelFactory.class).install();

        // Install group services for channel
        for (GroupServiceInstaller installer : ServiceLoader.load(ClusteredGroupServiceInstaller.class, ClusteredGroupServiceInstaller.class.getClassLoader())) {
            JGroupsLogger.ROOT_LOGGER.debugf("Installing %s for channel %s", installer.getClass().getSimpleName(), channelName);
            installer.install(target, channelName, module);
        }
    }

    static void removeRuntimeServices(OperationContext context, ModelNode operation, ModelNode model) {
        PathAddress channelAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
        String channelName = channelAddress.getLastElement().getValue();

        context.removeService(ChannelService.getServiceName(channelName));
        context.removeService(ChannelService.createChannelBinding(channelName).getBinderServiceName());
        context.removeService(ChannelService.getFactoryServiceName(channelName));
        context.removeService(ConnectedChannelService.getServiceName(channelName));

        context.removeService(ChannelFactoryService.getServiceName(channelName));
        context.removeService(ChannelFactoryService.createChannelFactoryBinding(channelName).getBinderServiceName());

        for (GroupServiceInstaller installer : ServiceLoader.load(ClusteredGroupServiceInstaller.class, ClusteredGroupServiceInstaller.class.getClassLoader())) {
            for (ServiceName serviceName : installer.getServiceNames(channelName)) {
                context.removeService(serviceName);
            }
        }
    }

    static ResourceDefinition createProtocolResourceDefinition(String protocolName) throws OperationFailedException {

        String className = ProtocolConfiguration.protocol_prefix + "." + protocolName;

        try {
            Class<? extends Protocol> protocolClass = Protocol.class.getClassLoader().loadClass(className).asSubclass(Protocol.class);

            SimpleResourceDescriptionResolver resolver = new SimpleResourceDescriptionResolver(protocolName, protocolClass.getSimpleName());
            ResourceBuilder builder = ResourceBuilder.Factory.create(ProtocolResourceDefinition.pathElement(protocolName), resolver);
            ProtocolMetricsHandler handler = new ProtocolMetricsHandler();

            for (Map.Entry<String, Attribute> entry: handler.findProtocolAttributes(protocolClass).entrySet()) {
                String name = entry.getKey();
                Attribute attribute = entry.getValue();
                FieldType type = FieldType.valueOf(attribute.getType());
                resolver.addDescription(name, attribute.getDescription());
                builder.addMetric(new SimpleAttributeDefinitionBuilder(name, type.getModelType()).setStorageRuntime().build(), handler);
            }

            return builder.build();
        } catch (ClassNotFoundException e) {
            throw JGroupsLogger.ROOT_LOGGER.unableToLoadProtocolClass(className);
        }
    }
}
