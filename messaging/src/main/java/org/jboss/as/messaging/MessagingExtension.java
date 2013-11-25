/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.messaging.Namespace.MESSAGING_1_0;
import static org.jboss.as.messaging.Namespace.MESSAGING_1_1;
import static org.jboss.as.messaging.Namespace.MESSAGING_1_2;
import static org.jboss.as.messaging.Namespace.MESSAGING_1_3;
import static org.jboss.as.messaging.Namespace.MESSAGING_1_4;
import static org.jboss.as.messaging.Namespace.MESSAGING_2_0;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.as.messaging.jms.ConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.JMSQueueDefinition;
import org.jboss.as.messaging.jms.JMSTopicDefinition;
import org.jboss.as.messaging.jms.PooledConnectionFactoryDefinition;
import org.jboss.as.messaging.jms.bridge.JMSBridgeDefinition;

/**
 * Domain extension that integrates HornetQ.
 *
 * <dl>
 *   <dt>AS 8.0.0</dt>
 *   <dd>
 *     <ul>
 *       <li>XML namespace: urn:jboss:domain:messaging:2.0
 *       <li>Management model: 2.0.0
 *     </ul>
 *   </dd>
 *   <dt>AS 7.3</dt>
 *   <dd>
 *     <ul>
 *       <li>XML namespace: urn:jboss:domain:messaging:1.4
 *       <li>Management model: 1.3.0
 *     </ul>
 *   </dd>
 *   <dt>AS 7.2.1</dt>
 *   <dd>
 *     <ul>
 *       <li>XML namespace: urn:jboss:domain:messaging:1.3
 *       <li>Management model: 1.2.1
 *     </ul>
 *   </dd>
 *   <dt>AS 7.2.0</dt>
 *   <dd>
 *     <ul>
 *       <li>XML namespace: urn:jboss:domain:messaging:1.3
 *       <li>Management model: 1.2.0
 *     </ul>
 *   </dd>
 *   <dt>AS 7.1.2, 7.1.3<dt>
 *   <dd>
 *     <ul>
 *       <li>XML namespace: urn:jboss:domain:messaging:1.2
 *       <li>Management model: 1.1.0
 *     </ul>
 *   </dd>
 * </dl>
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class MessagingExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "messaging";

    static final PathElement SUBSYSTEM_PATH  = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);

    static final String RESOURCE_NAME = MessagingExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final int MANAGEMENT_API_MAJOR_VERSION = 2;
    private static final int MANAGEMENT_API_MINOR_VERSION = 0;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    public static final ModelVersion VERSION_2_0_0 = ModelVersion.create(2, 0, 0);
    public static final ModelVersion VERSION_1_4_0 = ModelVersion.create(1, 4, 0);
    public static final ModelVersion VERSION_1_3_0 = ModelVersion.create(1, 3, 0);
    public static final ModelVersion VERSION_1_2_1 = ModelVersion.create(1, 2, 1);
    public static final ModelVersion VERSION_1_2_0 = ModelVersion.create(1, 2, 0);
    public static final ModelVersion VERSION_1_1_0 = ModelVersion.create(1, 1, 0);

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        return getResourceDescriptionResolver(true, keyPrefix);
    }

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final boolean useUnprefixedChildTypes, final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder();
        for (String kp : keyPrefix) {
            if (prefix.length() > 0){
                prefix.append('.');
            }
            prefix.append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, MessagingExtension.class.getClassLoader(), true, useUnprefixedChildTypes);
    }

    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME,
                MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION,
                MANAGEMENT_API_MICRO_VERSION);
        subsystem.registerXMLElementWriter(MessagingXMLWriter.INSTANCE);

        boolean registerRuntimeOnly = context.isRuntimeOnlyRegistrationValid();

        // Root resource
        final ManagementResourceRegistration rootRegistration = subsystem.registerSubsystemModel(MessagingSubsystemRootResourceDefinition.INSTANCE);
        rootRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        // HQ servers
        final ManagementResourceRegistration serverRegistration = rootRegistration.registerSubModel(new HornetQServerResourceDefinition(registerRuntimeOnly));

        // Runtime addresses
        if (registerRuntimeOnly) {
            final ManagementResourceRegistration coreAddress = serverRegistration.registerSubModel(new CoreAddressDefinition());
            coreAddress.setRuntimeOnly(true);
        }

        // Address settings
        serverRegistration.registerSubModel(new AddressSettingDefinition(registerRuntimeOnly));

        // Broadcast groups
        serverRegistration.registerSubModel(new BroadcastGroupDefinition(registerRuntimeOnly));
        // getConnectorPairs, -- no, this is just the same as attribute connector-refs

        // Discovery groups
        serverRegistration.registerSubModel(new DiscoveryGroupDefinition(registerRuntimeOnly));

        // Diverts
        serverRegistration.registerSubModel(new DivertDefinition(registerRuntimeOnly));

        // Core queues
        serverRegistration.registerSubModel(QueueDefinition.newQueueDefinition(registerRuntimeOnly));
        // getExpiryAddress, setExpiryAddress, getDeadLetterAddress, setDeadLetterAddress  -- no -- just toggle the 'queue-address', make this a mutable attr of address-setting

        // Runtime core queues
        serverRegistration.registerSubModel(QueueDefinition.newRuntimeQueueDefinition(registerRuntimeOnly));

        // Acceptors
        serverRegistration.registerSubModel(GenericTransportDefinition.createAcceptorDefinition(registerRuntimeOnly));
        serverRegistration.registerSubModel(RemoteTransportDefinition.createAcceptorDefinition(registerRuntimeOnly));
        serverRegistration.registerSubModel(InVMTransportDefinition.createAcceptorDefinition(registerRuntimeOnly));

        // Connectors
        serverRegistration.registerSubModel(GenericTransportDefinition.createConnectorDefinition(registerRuntimeOnly));
        serverRegistration.registerSubModel(RemoteTransportDefinition.createConnectorDefinition(registerRuntimeOnly));
        serverRegistration.registerSubModel(InVMTransportDefinition.createConnectorDefinition(registerRuntimeOnly));

        // Bridges
        serverRegistration.registerSubModel(new BridgeDefinition(registerRuntimeOnly));

        // Cluster connections
        serverRegistration.registerSubModel(new ClusterConnectionDefinition(registerRuntimeOnly));

        // Grouping Handler
        serverRegistration.registerSubModel(new GroupingHandlerDefinition(registerRuntimeOnly));

        // Connector services
        serverRegistration.registerSubModel(new ConnectorServiceDefinition(registerRuntimeOnly));

        // Messaging paths
        //todo, shouldn't we leverage Path service from AS? see: package org.jboss.as.controller.services.path
        for (final String path : MessagingPathHandlers.PATHS.keySet()) {
            ManagementResourceRegistration bindings = serverRegistration.registerSubModel(PathElement.pathElement(PATH, path),
                    new MessagingSubsystemProviders.PathProvider(path));
            MessagingPathHandlers.register(bindings, path);
            // Create the path resolver operation
            if (context.getProcessType().isServer()) {
                final ResolvePathHandler resolvePathHandler = ResolvePathHandler.Builder.of(context.getPathManager())
                        .setPathAttribute(MessagingPathHandlers.PATHS.get(path))
                        .setRelativeToAttribute(MessagingPathHandlers.RELATIVE_TO)
                        .build();
                bindings.registerOperationHandler(resolvePathHandler.getOperationDefinition(), resolvePathHandler);
            }
        }

        // Connection factories
        serverRegistration.registerSubModel(new ConnectionFactoryDefinition(registerRuntimeOnly));
        // getJNDIBindings (no -- same as "entries")

        // Resource Adapter Pooled connection factories
        serverRegistration.registerSubModel(new PooledConnectionFactoryDefinition(registerRuntimeOnly, false));
        // TODO how do ConnectionFactoryControl things relate?

        // JMS Queues
        serverRegistration.registerSubModel(new JMSQueueDefinition(registerRuntimeOnly));
        // setExpiryAddress, setDeadLetterAddress  -- no -- just toggle the 'queue-address', make this a mutable attr of address-setting
        // getJNDIBindings (no -- same as "entries")

        // JMS Topics
        serverRegistration.registerSubModel(new JMSTopicDefinition(registerRuntimeOnly));
        // getJNDIBindings (no -- same as "entries")

        serverRegistration.registerSubModel(new SecuritySettingDefinition(registerRuntimeOnly));

        if (registerRuntimeOnly) {

            ResourceDefinition deploymentsDef = new SimpleResourceDefinition(SUBSYSTEM_PATH, getResourceDescriptionResolver("deployed"));
            final ManagementResourceRegistration deploymentsRegistration = subsystem.registerDeploymentModel(deploymentsDef);
            final ManagementResourceRegistration serverModel = deploymentsRegistration.registerSubModel(new HornetQServerResourceDefinition(true));

            serverModel.registerSubModel(JMSQueueDefinition.newDeployedJMSQueueDefinition());
            serverModel.registerSubModel(JMSTopicDefinition.newDeployedJMSTopicDefinition());
            serverModel.registerSubModel(new PooledConnectionFactoryDefinition(true, true));
        }

        // JMS Bridges
        rootRegistration.registerSubModel(new JMSBridgeDefinition());

        if (context.isRegisterTransformers()) {
            MessagingTransformers.registerTransformers(subsystem);
        }
    }

    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MESSAGING_1_0.getUriString(), MessagingSubsystemParser.getInstance());
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MESSAGING_1_1.getUriString(), MessagingSubsystemParser.getInstance());
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MESSAGING_1_2.getUriString(), Messaging12SubsystemParser.getInstance());
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MESSAGING_1_3.getUriString(), Messaging13SubsystemParser.getInstance());
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MESSAGING_1_4.getUriString(), Messaging14SubsystemParser.getInstance());
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MESSAGING_2_0.getUriString(), Messaging20SubsystemParser.getInstance());
    }
}
