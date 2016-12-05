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

import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.messaging.Namespace.MESSAGING_1_0;
import static org.jboss.as.messaging.Namespace.MESSAGING_1_1;
import static org.jboss.as.messaging.Namespace.MESSAGING_1_2;
import static org.jboss.as.messaging.Namespace.MESSAGING_1_3;
import static org.jboss.as.messaging.Namespace.MESSAGING_1_4;
import static org.jboss.as.messaging.Namespace.MESSAGING_1_5;
import static org.jboss.as.messaging.Namespace.MESSAGING_2_0;
import static org.jboss.as.messaging.Namespace.MESSAGING_3_0;

import java.util.Collections;
import java.util.Set;

import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DeprecatedResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.extension.AbstractLegacyExtension;
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
 *   <dt><strong>Current</strong> - WildFly 9 / 10</dt>
 *   <dd>
 *     <ul>
 *       <li>XML namespace: urn:jboss:domain:messaging:3.0
 *       <li>Management model: 3.0.0
 *     </ul>
 *   </dd>
 *   <dt>WildFly 8.1.0</dt>
 *   <dd>
 *     <ul>
 *       <li>XML namespace: urn:jboss:domain:messaging:2.0
 *       <li>Management model: 2.1.0
 *     </ul>
 *   </dd>
 *   <dt>WildFly 8.0.0</dt>
 *   <dd>
 *     <ul>
 *       <li>XML namespace: urn:jboss:domain:messaging:2.0
 *       <li>Management model: 2.0.0
 *     </ul>
 *   </dd>
 *   <dt>EAP 6.4</dt>
 *   <dd>
 *     <ul>
 *       <li>XML namespace: urn:jboss:domain:messaging:1.5
 *       <li>Management model: 1.4.0
 *     </ul>
 *   </dd>
 *   <dt>AS 7.3.0</dt>
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
@SuppressWarnings("deprecation")
public class MessagingExtension extends AbstractLegacyExtension {

    public static final String SUBSYSTEM_NAME = "messaging";

    static final PathElement SUBSYSTEM_PATH  = pathElement(SUBSYSTEM, SUBSYSTEM_NAME);

    static final String RESOURCE_NAME = MessagingExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(3, 0, 0);

    public static final ModelVersion VERSION_2_1_0 = ModelVersion.create(2, 1, 0);
    public static final ModelVersion VERSION_2_0_0 = ModelVersion.create(2, 0, 0);
    public static final ModelVersion VERSION_1_4_0 = ModelVersion.create(1, 4, 0);
    public static final ModelVersion VERSION_1_3_0 = ModelVersion.create(1, 3, 0);
    public static final ModelVersion VERSION_1_2_0 = ModelVersion.create(1, 2, 0);
    public static final ModelVersion VERSION_1_1_0 = ModelVersion.create(1, 1, 0);
    public static final ModelVersion DEPRECATED_SINCE = ModelVersion.create(1, 4, 0);

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        return getResourceDescriptionResolver(true, keyPrefix);
    }

    @SuppressWarnings("deprecation")
    public static ResourceDescriptionResolver getResourceDescriptionResolver(final boolean useUnprefixedChildTypes, final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder();
        for (String kp : keyPrefix) {
            if (prefix.length() > 0){
                prefix.append('.');
            }
            prefix.append(kp);
        }
        return new DeprecatedResourceDescriptionResolver(SUBSYSTEM_NAME, prefix.toString(), RESOURCE_NAME, MessagingExtension.class.getClassLoader(), true, useUnprefixedChildTypes);
    }

    public MessagingExtension() {
        super("org.jboss.as.messaging", SUBSYSTEM_NAME);
    }

    @Override
    protected Set<ManagementResourceRegistration> initializeLegacyModel(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        subsystem.registerXMLElementWriter(MessagingXMLWriter.INSTANCE);

        // Root resource
        final ManagementResourceRegistration rootRegistration = subsystem.registerSubsystemModel(MessagingSubsystemRootResourceDefinition.INSTANCE);

        // HQ servers
        final ManagementResourceRegistration serverRegistration = rootRegistration.registerSubModel(HornetQServerResourceDefinition.INSTANCE);

        // Address settings
        serverRegistration.registerSubModel(AddressSettingDefinition.INSTANCE);

        // Broadcast groups
        serverRegistration.registerSubModel(BroadcastGroupDefinition.INSTANCE);

        // Discovery groups
        serverRegistration.registerSubModel(DiscoveryGroupDefinition.INSTANCE);

        // Diverts
        serverRegistration.registerSubModel(DivertDefinition.INSTANCE);

        // Core queues
        serverRegistration.registerSubModel(QueueDefinition.INSTANCE);

        // Acceptors
        serverRegistration.registerSubModel(HTTPAcceptorDefinition.INSTANCE);
        serverRegistration.registerSubModel(GenericTransportDefinition.ACCEPTOR_INSTANCE);
        serverRegistration.registerSubModel(RemoteTransportDefinition.ACCEPTOR_INSTANCE);
        serverRegistration.registerSubModel(InVMTransportDefinition.ACCEPTOR_INSTANCE);

        // Connectors
        serverRegistration.registerSubModel(HTTPConnectorDefinition.INSTANCE);
        serverRegistration.registerSubModel(GenericTransportDefinition.CONNECTOR_INSTANCE);
        serverRegistration.registerSubModel(RemoteTransportDefinition.CONNECTOR_INSTANCE);
        serverRegistration.registerSubModel(InVMTransportDefinition.CONNECTOR_INSTANCE);

        // Bridges
        serverRegistration.registerSubModel(BridgeDefinition.INSTANCE);

        // Cluster connections
        serverRegistration.registerSubModel(ClusterConnectionDefinition.INSTANCE);

        // Grouping Handler
        serverRegistration.registerSubModel(GroupingHandlerDefinition.INSTANCE);

        // Connector services
        serverRegistration.registerSubModel(ConnectorServiceDefinition.INSTANCE);

        // Messaging paths
        //todo, shouldn't we leverage Path service from AS? see: package org.jboss.as.controller.services.path
        for (final String path : PathDefinition.PATHS.keySet()) {
            ManagementResourceRegistration binding = serverRegistration.registerSubModel(new PathDefinition(pathElement(ModelDescriptionConstants.PATH, path)));
            // Create the path resolver operation
            if (context.getProcessType().isServer()) {
                final ResolvePathHandler resolvePathHandler = ResolvePathHandler.Builder.of(context.getPathManager())
                        .setPathAttribute(PathDefinition.PATHS.get(path))
                        .setRelativeToAttribute(PathDefinition.RELATIVE_TO)
                        .build();
                binding.registerOperationHandler(resolvePathHandler.getOperationDefinition(), resolvePathHandler);
            }
        }

        // Connection factories
        serverRegistration.registerSubModel(ConnectionFactoryDefinition.INSTANCE);

        // Resource Adapter Pooled connection factories
        serverRegistration.registerSubModel(PooledConnectionFactoryDefinition.INSTANCE);

        // JMS Queues
        serverRegistration.registerSubModel(JMSQueueDefinition.INSTANCE);
        // JMS Topics
        serverRegistration.registerSubModel(JMSTopicDefinition.INSTANCE);

        serverRegistration.registerSubModel(SecuritySettingDefinition.INSTANCE);

        // JMS Bridges
        rootRegistration.registerSubModel(JMSBridgeDefinition.INSTANCE);

        if (context.isRegisterTransformers()) {
            MessagingTransformers.registerTransformers(subsystem);
        }

        return Collections.singleton(rootRegistration);
    }

    @Override
    protected void initializeLegacyParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MESSAGING_1_0.getUriString(), MessagingSubsystemParser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MESSAGING_1_1.getUriString(), MessagingSubsystemParser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MESSAGING_1_2.getUriString(), Messaging12SubsystemParser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MESSAGING_1_3.getUriString(), Messaging13SubsystemParser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MESSAGING_1_4.getUriString(), Messaging14SubsystemParser::new);
        // the 1.5 schema is port forwarded from EAP 6.4.
        // The 1.4 schema was updated by mistake in EAP 6.4. The 1.4 parser in WildFly is updated to be able to parse these
        // elements. There are no other changes in the 1.5 schema apart from these elements so we use the 1.4 parser to parse
        // the 1.5 schema too.
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MESSAGING_1_5.getUriString(), Messaging14SubsystemParser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MESSAGING_2_0.getUriString(), Messaging20SubsystemParser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MESSAGING_3_0.getUriString(), Messaging30SubsystemParser::new);
    }
}