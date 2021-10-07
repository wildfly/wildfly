/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq;

import static org.apache.activemq.artemis.api.core.client.ActiveMQClient.SCHEDULED_THREAD_POOL_SIZE_PROPERTY_KEY;
import static org.apache.activemq.artemis.api.core.client.ActiveMQClient.THREAD_POOL_MAX_SIZE_PROPERTY_KEY;
import static org.jboss.as.server.services.net.SocketBindingResourceDefinition.SOCKET_BINDING_CAPABILITY;
import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;
import static org.wildfly.extension.messaging.activemq.Capabilities.OUTBOUND_SOCKET_BINDING_CAPABILITY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.BROADCAST_GROUP;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.DISCOVERY_GROUP;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JGROUPS_CLUSTER;
import static org.wildfly.extension.messaging.activemq.MessagingSubsystemRootResourceDefinition.CONFIGURATION_CAPABILITY;
import static org.wildfly.extension.messaging.activemq.MessagingSubsystemRootResourceDefinition.GLOBAL_CLIENT_SCHEDULED_THREAD_POOL_MAX_SIZE;
import static org.wildfly.extension.messaging.activemq.MessagingSubsystemRootResourceDefinition.GLOBAL_CLIENT_THREAD_POOL_MAX_SIZE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;

import org.apache.activemq.artemis.api.core.BroadcastGroupConfiguration;
import org.apache.activemq.artemis.api.core.DiscoveryGroupConfiguration;
import org.apache.activemq.artemis.api.core.TransportConfiguration;

import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.messaging.activemq.deployment.DefaultJMSConnectionFactoryBindingProcessor;
import org.wildfly.extension.messaging.activemq.deployment.DefaultJMSConnectionFactoryResourceReferenceProcessor;
import org.wildfly.extension.messaging.activemq.deployment.JMSConnectionFactoryDefinitionAnnotationProcessor;
import org.wildfly.extension.messaging.activemq.deployment.JMSConnectionFactoryDefinitionDescriptorProcessor;
import org.wildfly.extension.messaging.activemq.deployment.JMSDestinationDefinitionAnnotationProcessor;
import org.wildfly.extension.messaging.activemq.deployment.JMSDestinationDefinitionDescriptorProcessor;
import org.wildfly.extension.messaging.activemq.deployment.MessagingDependencyProcessor;
import org.wildfly.extension.messaging.activemq.deployment.MessagingXmlInstallDeploymentUnitProcessor;
import org.wildfly.extension.messaging.activemq.deployment.MessagingXmlParsingDeploymentUnitProcessor;
import org.wildfly.extension.messaging.activemq.deployment.injection.CDIDeploymentProcessor;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 * Add handler for the messaging subsystem.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class MessagingSubsystemAdd extends AbstractBoottimeAddStepHandler {

    private static final ServiceName JBOSS_MESSAGING_ACTIVEMQ = ServiceName.JBOSS.append(MessagingExtension.SUBSYSTEM_NAME);

    final BiConsumer<OperationContext, String> broadcastCommandDispatcherFactoryInstaller;

    MessagingSubsystemAdd(BiConsumer<OperationContext, String> broadcastCommandDispatcherFactoryInstaller) {
        super(MessagingSubsystemRootResourceDefinition.ATTRIBUTES);
        this.broadcastCommandDispatcherFactoryInstaller = broadcastCommandDispatcherFactoryInstaller;
    }

    @Override
    protected void performBoottime(final OperationContext context, ModelNode operation, final ModelNode model) throws OperationFailedException {

        // Cache support for capability service name lookups by our services
        MessagingServices.capabilityServiceSupport = context.getCapabilityServiceSupport();

        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            protected void execute(DeploymentProcessorTarget processorTarget) {
                // keep the statements ordered by phase + priority
                processorTarget.addDeploymentProcessor(MessagingExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_JMS_CONNECTION_FACTORY_RESOURCE_INJECTION, new DefaultJMSConnectionFactoryResourceReferenceProcessor());
                processorTarget.addDeploymentProcessor(MessagingExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_RESOURCE_DEF_ANNOTATION_JMS_DESTINATION, new JMSDestinationDefinitionAnnotationProcessor());
                processorTarget.addDeploymentProcessor(MessagingExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_RESOURCE_DEF_ANNOTATION_JMS_CONNECTION_FACTORY, new JMSConnectionFactoryDefinitionAnnotationProcessor(MessagingServices.capabilityServiceSupport.hasCapability("org.wildfly.legacy-security")));
                processorTarget.addDeploymentProcessor(MessagingExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_MESSAGING_XML_RESOURCES, new MessagingXmlParsingDeploymentUnitProcessor());
                processorTarget.addDeploymentProcessor(MessagingExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_JMS, new MessagingDependencyProcessor());

                if (MessagingServices.capabilityServiceSupport.hasCapability(WELD_CAPABILITY_NAME)) {
                    processorTarget.addDeploymentProcessor(MessagingExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_JMS_CDI_EXTENSIONS, new CDIDeploymentProcessor());
                }

                processorTarget.addDeploymentProcessor(MessagingExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_RESOURCE_DEF_XML_JMS_CONNECTION_FACTORY, new JMSConnectionFactoryDefinitionDescriptorProcessor());
                processorTarget.addDeploymentProcessor(MessagingExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_RESOURCE_DEF_XML_JMS_DESTINATION, new JMSDestinationDefinitionDescriptorProcessor());
                processorTarget.addDeploymentProcessor(MessagingExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_DEFAULT_BINDINGS_JMS_CONNECTION_FACTORY, new DefaultJMSConnectionFactoryBindingProcessor());
                processorTarget.addDeploymentProcessor(MessagingExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_MESSAGING_XML_RESOURCES, new MessagingXmlInstallDeploymentUnitProcessor());
            }
        }, OperationContext.Stage.RUNTIME);

        ModelNode threadPoolMaxSize = operation.get(GLOBAL_CLIENT_THREAD_POOL_MAX_SIZE.getName());
        ModelNode scheduledThreadPoolMaxSize = operation.get(GLOBAL_CLIENT_SCHEDULED_THREAD_POOL_MAX_SIZE.getName());
        Integer threadPoolMaxSizeValue;
        Integer scheduledThreadPoolMaxSizeValue;

        // if the attributes are defined, their value is used (and the system properties are ignored)
        Properties sysprops = System.getProperties();

        if (threadPoolMaxSize.isDefined()) {
            threadPoolMaxSizeValue = GLOBAL_CLIENT_THREAD_POOL_MAX_SIZE.resolveModelAttribute(context, operation).asInt();
        } else if (sysprops.containsKey(THREAD_POOL_MAX_SIZE_PROPERTY_KEY)) {
            threadPoolMaxSizeValue = Integer.parseInt(sysprops.getProperty(THREAD_POOL_MAX_SIZE_PROPERTY_KEY));
        } else {
            // property is not configured using sysprop or explicit attribute
            threadPoolMaxSizeValue = null;
        }

        if (scheduledThreadPoolMaxSize.isDefined()) {
            scheduledThreadPoolMaxSizeValue = GLOBAL_CLIENT_SCHEDULED_THREAD_POOL_MAX_SIZE.resolveModelAttribute(context, operation).asInt();
        } else if (sysprops.containsKey(SCHEDULED_THREAD_POOL_SIZE_PROPERTY_KEY)) {
            scheduledThreadPoolMaxSizeValue = Integer.parseInt(sysprops.getProperty(SCHEDULED_THREAD_POOL_SIZE_PROPERTY_KEY));
        } else {
            // property is not configured using sysprop or explicit attribute
            scheduledThreadPoolMaxSizeValue = null;
        }

        if (threadPoolMaxSizeValue != null || scheduledThreadPoolMaxSizeValue != null) {
            ActiveMQClient.initializeGlobalThreadPoolProperties();
            if(threadPoolMaxSizeValue == null) {
                threadPoolMaxSizeValue = ActiveMQClient.getGlobalThreadPoolSize();
            }
            if(scheduledThreadPoolMaxSizeValue == null) {
                scheduledThreadPoolMaxSizeValue = ActiveMQClient.getGlobalScheduledThreadPoolSize();
            }
            MessagingLogger.ROOT_LOGGER.debugf("Setting global client thread pool size to: regular=%s, scheduled=%s", threadPoolMaxSizeValue, scheduledThreadPoolMaxSizeValue);
            ActiveMQClient.setGlobalThreadPoolProperties(threadPoolMaxSizeValue, scheduledThreadPoolMaxSizeValue);
        }
        context.getServiceTarget().addService(MessagingServices.ACTIVEMQ_CLIENT_THREAD_POOL)
                .setInstance( new ThreadPoolService())
                .install();
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                final ServiceTarget serviceTarget = context.getServiceTarget();
                final ServiceBuilder serviceBuilder = serviceTarget.addService(CONFIGURATION_CAPABILITY.getCapabilityServiceName());
                // Transform the configuration based on the recursive model
                final ModelNode model = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
                // Process connectors
                final Set<String> connectorsSocketBindings = new HashSet<String>();
                final Map<String, TransportConfiguration> connectors = TransportConfigOperationHandlers.processConnectors(context, "localhost", model, connectorsSocketBindings);

                Map<String, ServiceName> outboundSocketBindings = new HashMap<>();
                Map<String, Boolean> outbounds = TransportConfigOperationHandlers.listOutBoundSocketBinding(context, connectorsSocketBindings);
                Map<String, ServiceName> socketBindings = new HashMap<>();
                for (final String connectorSocketBinding : connectorsSocketBindings) {
                    // find whether the connectorSocketBinding references a SocketBinding or an OutboundSocketBinding
                    if (outbounds.get(connectorSocketBinding)) {
                        final ServiceName outboundSocketName = OUTBOUND_SOCKET_BINDING_CAPABILITY.getCapabilityServiceName(connectorSocketBinding);
                        outboundSocketBindings.put(connectorSocketBinding, outboundSocketName);
                    } else {
                        // check if the socket binding has not already been added by the acceptors
                        if (!socketBindings.containsKey(connectorSocketBinding)) {
                            socketBindings.put(connectorSocketBinding, SOCKET_BINDING_CAPABILITY.getCapabilityServiceName(connectorSocketBinding));
                        }
                    }
                }
                final List<BroadcastGroupConfiguration> broadcastGroupConfigurations =new ArrayList<>();
                //this requires connectors
                BroadcastGroupAdd.addBroadcastGroupConfigs(context, broadcastGroupConfigurations, connectors.keySet(), model);
                final Map<String, DiscoveryGroupConfiguration> discoveryGroupConfigurations = ConfigurationHelper.addDiscoveryGroupConfigurations(context, model);
                final Map<String, String> clusterNames = new HashMap<>();
                final Map<String, ServiceName> commandDispatcherFactories = new HashMap<>();
                final Map<String, ServiceName> groupBindings = new HashMap<>();
                final Set<ServiceName> groupBindingServices = new HashSet<>();
                for (final BroadcastGroupConfiguration config : broadcastGroupConfigurations) {
                    final String name = config.getName();
                    final String key = "broadcast" + name;
                    ModelNode broadcastGroupModel = model.get(BROADCAST_GROUP, name);

                    if (broadcastGroupModel.hasDefined(JGROUPS_CLUSTER.getName())) {
                        String channelName = JGroupsBroadcastGroupDefinition.JGROUPS_CHANNEL.resolveModelAttribute(context, broadcastGroupModel).asStringOrNull();
                        MessagingSubsystemAdd.this.broadcastCommandDispatcherFactoryInstaller.accept(context, channelName);
                        commandDispatcherFactories.put(key, MessagingServices.getBroadcastCommandDispatcherFactoryServiceName(channelName));
                        String clusterName = JGROUPS_CLUSTER.resolveModelAttribute(context, broadcastGroupModel).asString();
                        clusterNames.put(key, clusterName);
                    } else {
                        final ServiceName groupBindingServiceName = GroupBindingService.getBroadcastBaseServiceName(JBOSS_MESSAGING_ACTIVEMQ).append(name);
                        if (!groupBindingServices.contains(groupBindingServiceName)) {
                            groupBindingServices.add(groupBindingServiceName);
                        }
                        groupBindings.put(key, groupBindingServiceName);
                    }
                }
                for (final DiscoveryGroupConfiguration config : discoveryGroupConfigurations.values()) {
                    final String name = config.getName();
                    final String key = "discovery" + name;
                    ModelNode discoveryGroupModel = model.get(DISCOVERY_GROUP, name);
                    if (discoveryGroupModel.hasDefined(JGROUPS_CLUSTER.getName())) {
                        String channelName = JGroupsDiscoveryGroupDefinition.JGROUPS_CHANNEL.resolveModelAttribute(context, discoveryGroupModel).asStringOrNull();
                        MessagingSubsystemAdd.this.broadcastCommandDispatcherFactoryInstaller.accept(context, channelName);
                        commandDispatcherFactories.put(key, MessagingServices.getBroadcastCommandDispatcherFactoryServiceName(channelName));
                        String clusterName = JGROUPS_CLUSTER.resolveModelAttribute(context, discoveryGroupModel).asString();
                        clusterNames.put(key, clusterName);
                    } else {
                        final ServiceName groupBindingServiceName = GroupBindingService.getDiscoveryBaseServiceName(JBOSS_MESSAGING_ACTIVEMQ).append(name);
                        if (!groupBindingServices.contains(groupBindingServiceName)) {
                            groupBindingServices.add(groupBindingServiceName);
                        }
                        groupBindings.put(key, groupBindingServiceName);
                    }
                }
                serviceBuilder.setInstance(new ExternalBrokerConfigurationService(
                        connectors,
                        discoveryGroupConfigurations,
                        socketBindings,
                        outboundSocketBindings,
                        groupBindings,
                        commandDispatcherFactories,
                        clusterNames))
                        .install();
            }
        }, OperationContext.Stage.RUNTIME);
    }

    /**
     * Service to ensure that Artemis global client thread pools have the opportunity to shutdown when the server is
     * stopped (or the subsystem is removed).
     */
    private static class ThreadPoolService implements Service<Void> {

        public ThreadPoolService() {
        }

        @Override
        public void start(StartContext startContext) throws StartException {
        }

        @Override
        public void stop(StopContext stopContext) {
            ActiveMQClient.clearThreadPools();
        }

        @Override
        public Void getValue() throws IllegalStateException, IllegalArgumentException {
            return null;
        }
    }
}
