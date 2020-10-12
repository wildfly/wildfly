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

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.PathElement.pathElement;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.ADDRESS_SETTING;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.BINDINGS_DIRECTORY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.BRIDGE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.BROADCAST_GROUP;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CLUSTER_CONNECTION;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CONFIGURATION;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CONNECTOR_SERVICE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.GROUPING_HANDLER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.HA_POLICY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.HTTP_ACCEPTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.HTTP_CONNECTOR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JMS_QUEUE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JMS_TOPIC;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JOURNAL_DIRECTORY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.LARGE_MESSAGES_DIRECTORY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.LEGACY_CONNECTION_FACTORY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.LIVE_ONLY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.MASTER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.PAGING_DIRECTORY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.QUEUE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.REPLICATION_COLOCATED;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.REPLICATION_MASTER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.REPLICATION_SLAVE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.ROLE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.RUNTIME_QUEUE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SECURITY_SETTING;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SERVER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SHARED_STORE_MASTER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SHARED_STORE_SLAVE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SLAVE;

import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.JdkLoggerFactory;
import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition.Parameters;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.ApplicationTypeAccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.extension.messaging.activemq.jms.ExternalConnectionFactoryDefinition;
import org.wildfly.extension.messaging.activemq.jms.ExternalJMSQueueDefinition;
import org.wildfly.extension.messaging.activemq.jms.ExternalJMSTopicDefinition;
import org.wildfly.extension.messaging.activemq.jms.JMSQueueDefinition;
import org.wildfly.extension.messaging.activemq.jms.JMSTopicDefinition;
import org.wildfly.extension.messaging.activemq.jms.PooledConnectionFactoryDefinition;
import org.wildfly.extension.messaging.activemq.jms.bridge.JMSBridgeDefinition;

import static org.wildfly.extension.messaging.activemq.CommonAttributes.EXTERNAL_JMS_QUEUE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.EXTERNAL_JMS_TOPIC;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JGROUPS_BROADCAST_GROUP;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SOCKET_BROADCAST_GROUP;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.wildfly.extension.messaging.activemq.jms.ExternalPooledConnectionFactoryDefinition;

/**
 * Domain extension that integrates Apache ActiveMQ 6.
 *
 * <dl>
 * <dt><strong>Current</strong> - WildFly 22</dt>
 *   <dd>
 *     <ul>
 *       <li>XML namespace: urn:jboss:domain:messaging-activemq:12.0
 *       <li>Management model: 12.0.0
 *     </ul>
 *   </dd>
 * <dt><strong>Current</strong> - WildFly 21</dt>
 *   <dd>
 *     <ul>
 *       <li>XML namespace: urn:jboss:domain:messaging-activemq:11.0
 *       <li>Management model: 11.0.0
 *     </ul>
 *   </dd>
 * <dt>WildFly 20</dt>
 *   <dd>
 *     <ul>
 *       <li>XML namespace: urn:jboss:domain:messaging-activemq:10.0
 *       <li>Management model: 10.0.0
 *     </ul>
 *   </dd>
 * <dt>WildFly 19</dt>
 *  *   <dd>
 *  *     <ul>
 *  *       <li>XML namespace: urn:jboss:domain:messaging-activemq:9.0
 *  *       <li>Management model: 9.0.0
 *  *     </ul>
 *  *   </dd>
 * <dt>WildFly 18</dt>
 *   <dd>
 *     <ul>
 *       <li>XML namespace: urn:jboss:domain:messaging-activemq:8.0
 *       <li>Management model: 8.0.0
 *     </ul>
 *   </dd>
 * <dt>WildFly 17</dt>
 *   <dd>
 *     <ul>
 *       <li>XML namespace: urn:jboss:domain:messaging-activemq:7.0
 *       <li>Management model: 7.0.0
 *     </ul>
 *   </dd>
 * <dt>WildFly 16</dt>
 *   <dd>
 *     <ul>
 *       <li>XML namespace: urn:jboss:domain:messaging-activemq:6.0
 *       <li>Management model: 6.0.0
 *     </ul>
 *   </dd>
 *  <dt>WildFly 15</dt>
 *   <dd>
 *     <ul>
 *       <li>XML namespace: urn:jboss:domain:messaging-activemq:5.0
 *       <li>Management model: 5.0.0
 *     </ul>
 *   </dd>
 *  <dt>WildFly 14</dt>
 *   <dd>
 *     <ul>
 *       <li>XML namespace: urn:jboss:domain:messaging-activemq:4.0
 *       <li>Management model: 4.0.0
 *     </ul>
 *   </dd>
 *   <dt>WildFly 12</dt>
 *   <dd>
 *     <ul>
 *       <li>XML namespace: urn:jboss:domain:messaging-activemq:3.0
 *       <li>Management model: 3.0.0
 *     </ul>
 *   </dd>
 *   <dt>WildFly 11</dt>
 *   <dd>
 *     <ul>
 *       <li>XML namespace: urn:jboss:domain:messaging-activemq:2.0
 *       <li>Management model: 2.0.0
 *     </ul>
 *   </dd>
 *   <dt>WildFly 10</dt>
 *   <dd>
 *     <ul>
 *       <li>XML namespace: urn:jboss:domain:messaging-activemq:1.0
 *       <li>Management model: 1.0.0
 *     </ul>
 *   </dd>
 * </dl>
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class MessagingExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "messaging-activemq";

    static final PathElement SUBSYSTEM_PATH  = pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    static final PathElement SERVER_PATH = pathElement(SERVER);
    public static final PathElement LIVE_ONLY_PATH = pathElement(HA_POLICY, LIVE_ONLY);
    public static final PathElement REPLICATION_MASTER_PATH = pathElement(HA_POLICY, REPLICATION_MASTER);
    public static final PathElement REPLICATION_SLAVE_PATH = pathElement(HA_POLICY, REPLICATION_SLAVE);
    public static final PathElement SHARED_STORE_MASTER_PATH = pathElement(HA_POLICY, SHARED_STORE_MASTER);
    public static final PathElement SHARED_STORE_SLAVE_PATH = pathElement(HA_POLICY, SHARED_STORE_SLAVE);
    public static final PathElement REPLICATION_COLOCATED_PATH = pathElement(HA_POLICY, REPLICATION_COLOCATED);
    public static final PathElement CONFIGURATION_MASTER_PATH = pathElement(CONFIGURATION, MASTER);
    public static final PathElement CONFIGURATION_SLAVE_PATH = pathElement(CONFIGURATION, SLAVE);
    static final PathElement BINDINGS_DIRECTORY_PATH = pathElement(PATH, BINDINGS_DIRECTORY);
    static final PathElement JOURNAL_DIRECTORY_PATH = pathElement(PATH, JOURNAL_DIRECTORY);
    static final PathElement PAGING_DIRECTORY_PATH = pathElement(PATH, PAGING_DIRECTORY);
    static final PathElement LARGE_MESSAGES_DIRECTORY_PATH = pathElement(PATH, LARGE_MESSAGES_DIRECTORY);
    static final PathElement CONNECTOR_SERVICE_PATH = pathElement(CONNECTOR_SERVICE);
    static final PathElement QUEUE_PATH = pathElement(QUEUE);
    static final PathElement RUNTIME_QUEUE_PATH = pathElement(RUNTIME_QUEUE);
    static final PathElement GROUPING_HANDLER_PATH = pathElement(GROUPING_HANDLER);
    static final PathElement HTTP_CONNECTOR_PATH = pathElement(HTTP_CONNECTOR);
    static final PathElement HTTP_ACCEPTOR_PATH = pathElement(HTTP_ACCEPTOR);
    static final PathElement BROADCAST_GROUP_PATH = pathElement(BROADCAST_GROUP);
    static final PathElement JGROUPS_BROADCAST_GROUP_PATH = pathElement(JGROUPS_BROADCAST_GROUP);
    static final PathElement SOCKET_BROADCAST_GROUP_PATH = pathElement(SOCKET_BROADCAST_GROUP);
    static final PathElement CLUSTER_CONNECTION_PATH = pathElement(CLUSTER_CONNECTION);
    static final PathElement BRIDGE_PATH = pathElement(BRIDGE);
    static final PathElement ADDRESS_SETTING_PATH = pathElement(ADDRESS_SETTING);
    static final PathElement ROLE_PATH = pathElement(ROLE);
    static final PathElement SECURITY_SETTING_PATH =  pathElement(SECURITY_SETTING);
    public static final PathElement EXTERNAL_JMS_QUEUE_PATH = pathElement(EXTERNAL_JMS_QUEUE);
    public static final PathElement EXTERNAL_JMS_TOPIC_PATH = pathElement(EXTERNAL_JMS_TOPIC);
    public static final PathElement JMS_QUEUE_PATH = pathElement(JMS_QUEUE);
    public static final PathElement JMS_TOPIC_PATH = pathElement(JMS_TOPIC);
    public static final PathElement POOLED_CONNECTION_FACTORY_PATH = pathElement(CommonAttributes.POOLED_CONNECTION_FACTORY);
    public static final PathElement CONNECTION_FACTORY_PATH = pathElement(CommonAttributes.CONNECTION_FACTORY);
    public static final PathElement JMS_BRIDGE_PATH = pathElement(CommonAttributes.JMS_BRIDGE);
    public static final PathElement LEGACY_CONNECTION_FACTORY_PATH = pathElement(LEGACY_CONNECTION_FACTORY);

    public static final SensitiveTargetAccessConstraintDefinition MESSAGING_SECURITY_SENSITIVE_TARGET = new SensitiveTargetAccessConstraintDefinition(new SensitivityClassification(SUBSYSTEM_NAME, "messaging-security", false, false, true));
    public static final SensitiveTargetAccessConstraintDefinition MESSAGING_MANAGEMENT_SENSITIVE_TARGET = new SensitiveTargetAccessConstraintDefinition(new SensitivityClassification(SUBSYSTEM_NAME, "messaging-management", false, false, true));

    static final AccessConstraintDefinition SECURITY_SETTING_ACCESS_CONSTRAINT = new ApplicationTypeAccessConstraintDefinition( new ApplicationTypeConfig(SUBSYSTEM_NAME, SECURITY_SETTING));
    static final AccessConstraintDefinition QUEUE_ACCESS_CONSTRAINT = new ApplicationTypeAccessConstraintDefinition(new ApplicationTypeConfig(SUBSYSTEM_NAME, QUEUE));
    public static final AccessConstraintDefinition JMS_QUEUE_ACCESS_CONSTRAINT = new ApplicationTypeAccessConstraintDefinition(new ApplicationTypeConfig(SUBSYSTEM_NAME, CommonAttributes.JMS_QUEUE));
    public static final AccessConstraintDefinition JMS_TOPIC_ACCESS_CONSTRAINT = new ApplicationTypeAccessConstraintDefinition(new ApplicationTypeConfig(SUBSYSTEM_NAME, CommonAttributes.JMS_TOPIC));

    static final String RESOURCE_NAME = MessagingExtension.class.getPackage().getName() + ".LocalDescriptions";

    protected static final ModelVersion VERSION_12_0_0 = ModelVersion.create(12, 0, 0);
    protected static final ModelVersion VERSION_11_0_0 = ModelVersion.create(11, 0, 0);
    protected static final ModelVersion VERSION_10_0_0 = ModelVersion.create(10, 0, 0);
    protected static final ModelVersion VERSION_9_0_0 = ModelVersion.create(9, 0, 0);
    protected static final ModelVersion VERSION_8_0_0 = ModelVersion.create(8, 0, 0);
    protected static final ModelVersion VERSION_7_0_0 = ModelVersion.create(7, 0, 0);
    protected static final ModelVersion VERSION_6_0_0 = ModelVersion.create(6, 0, 0);
    protected static final ModelVersion VERSION_5_0_0 = ModelVersion.create(5, 0, 0);
    protected static final ModelVersion VERSION_4_0_0 = ModelVersion.create(4, 0, 0);
    protected static final ModelVersion VERSION_3_0_0 = ModelVersion.create(3, 0, 0);
    protected static final ModelVersion VERSION_2_0_0 = ModelVersion.create(2, 0, 0);
    protected static final ModelVersion VERSION_1_0_0 = ModelVersion.create(1, 0, 0);
    private static final ModelVersion CURRENT_MODEL_VERSION = VERSION_12_0_0;

    private static final MessagingSubsystemParser_12_0 CURRENT_PARSER = new MessagingSubsystemParser_12_0();

    // ARTEMIS-2273 introduced audit logging at a info level which is rather verbose. We need to use static loggers
    // to ensure the log levels are set to WARN and there is a strong reference to the loggers. This hack will likely
    // be removed in the future.
    private static final Logger BASE_AUDIT_LOGGER;
    private static final Logger MESSAGE_AUDIT_LOGGER;

    static {
        // There is no guarantee that the configured loggers will contain the logger names even if they've been
        // configured, however it's unlikely it will be an issue. Checking the logger names is to avoid overriding
        // the loggers if they were previously configured.
        final Collection<String> configuredLoggers = Collections.list(LogManager.getLogManager().getLoggerNames());
        if (configuredLoggers.contains("org.apache.activemq.audit.base")) {
            BASE_AUDIT_LOGGER = null;
        } else {
            BASE_AUDIT_LOGGER = Logger.getLogger("org.apache.activemq.audit.base");
            BASE_AUDIT_LOGGER.setLevel(Level.WARNING);
        }
        if (configuredLoggers.contains("org.apache.activemq.audit.message")) {
            MESSAGE_AUDIT_LOGGER = null;
        } else {
            MESSAGE_AUDIT_LOGGER = Logger.getLogger("org.apache.activemq.audit.message");
            MESSAGE_AUDIT_LOGGER.setLevel(Level.WARNING);
        }
    }


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

    @Override
    public void initialize(ExtensionContext context) {
        // Initialize the Netty logger factory
        InternalLoggerFactory.setDefaultFactory(JdkLoggerFactory.INSTANCE);
        final SubsystemRegistration subsystemRegistration = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        subsystemRegistration.registerXMLElementWriter(CURRENT_PARSER);

        boolean registerRuntimeOnly = context.isRuntimeOnlyRegistrationValid();

        // Root resource
        final ManagementResourceRegistration subsystem = subsystemRegistration.registerSubsystemModel(MessagingSubsystemRootResourceDefinition.INSTANCE);
        subsystem.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        // WFLY-10518 - register new client resources under subsystem
        subsystem.registerSubModel(new DiscoveryGroupDefinition(registerRuntimeOnly, true));
        subsystem.registerSubModel(new JGroupsDiscoveryGroupDefinition(registerRuntimeOnly, true));
        subsystem.registerSubModel(new SocketDiscoveryGroupDefinition(registerRuntimeOnly, true));
        subsystem.registerSubModel(GenericTransportDefinition.createConnectorDefinition(registerRuntimeOnly));
        subsystem.registerSubModel(InVMTransportDefinition.createConnectorDefinition(registerRuntimeOnly));
        subsystem.registerSubModel(RemoteTransportDefinition.createConnectorDefinition(registerRuntimeOnly));
        subsystem.registerSubModel(new HTTPConnectorDefinition(registerRuntimeOnly));
        subsystem.registerSubModel(new ExternalConnectionFactoryDefinition(registerRuntimeOnly));
        subsystem.registerSubModel(ExternalPooledConnectionFactoryDefinition.INSTANCE);
        subsystem.registerSubModel(new ExternalJMSQueueDefinition(registerRuntimeOnly));
        subsystem.registerSubModel(new ExternalJMSTopicDefinition(registerRuntimeOnly));

        // ActiveMQ Servers
        final ManagementResourceRegistration server = subsystem.registerSubModel(new ServerDefinition(registerRuntimeOnly));

        for (PathDefinition path : new PathDefinition[] {
                PathDefinition.JOURNAL_INSTANCE,
                PathDefinition.BINDINGS_INSTANCE,
                PathDefinition.LARGE_MESSAGES_INSTANCE,
                PathDefinition.PAGING_INSTANCE
        }) {
            ManagementResourceRegistration pathRegistry = server.registerSubModel(path);
            PathDefinition.registerResolveOperationHandler(context, pathRegistry);
        }

        subsystem.registerSubModel(JMSBridgeDefinition.INSTANCE);

        if (registerRuntimeOnly) {
            final ManagementResourceRegistration deployment = subsystemRegistration.registerDeploymentModel(new SimpleResourceDefinition(
                    new Parameters(SUBSYSTEM_PATH, getResourceDescriptionResolver("deployed")).setFeature(false)));
            deployment.registerSubModel(new ExternalConnectionFactoryDefinition(registerRuntimeOnly));
            deployment.registerSubModel(ExternalPooledConnectionFactoryDefinition.DEPLOYMENT_INSTANCE);
            deployment.registerSubModel(new ExternalJMSQueueDefinition(registerRuntimeOnly));
            deployment.registerSubModel(new ExternalJMSTopicDefinition(registerRuntimeOnly));
            final ManagementResourceRegistration deployedServer = deployment.registerSubModel(new SimpleResourceDefinition(
                    new Parameters(SERVER_PATH, getResourceDescriptionResolver(SERVER)).setFeature(false)));
            deployedServer.registerSubModel(new JMSQueueDefinition(true, registerRuntimeOnly));
            deployedServer.registerSubModel(new JMSTopicDefinition(true, registerRuntimeOnly));
            deployedServer.registerSubModel(PooledConnectionFactoryDefinition.DEPLOYMENT_INSTANCE);
        }
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        // use method references for legacay versions that may never be instantiated
        // and use an instance for the current version that will be use if the extension is loaded.
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MessagingSubsystemParser_1_0.NAMESPACE, MessagingSubsystemParser_1_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MessagingSubsystemParser_2_0.NAMESPACE, MessagingSubsystemParser_2_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MessagingSubsystemParser_3_0.NAMESPACE, MessagingSubsystemParser_3_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MessagingSubsystemParser_4_0.NAMESPACE, MessagingSubsystemParser_4_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MessagingSubsystemParser_5_0.NAMESPACE, MessagingSubsystemParser_5_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MessagingSubsystemParser_6_0.NAMESPACE, MessagingSubsystemParser_6_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MessagingSubsystemParser_7_0.NAMESPACE, MessagingSubsystemParser_7_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MessagingSubsystemParser_8_0.NAMESPACE, MessagingSubsystemParser_8_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MessagingSubsystemParser_9_0.NAMESPACE, MessagingSubsystemParser_9_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MessagingSubsystemParser_10_0.NAMESPACE, MessagingSubsystemParser_10_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MessagingSubsystemParser_11_0.NAMESPACE, MessagingSubsystemParser_11_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MessagingSubsystemParser_12_0.NAMESPACE, CURRENT_PARSER);
    }
}
