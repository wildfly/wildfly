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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.messaging.CommonAttributes.QUEUE;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.messaging.jms.ConnectionFactoryAdd;
import org.jboss.as.messaging.jms.ConnectionFactoryRemove;
import org.jboss.as.messaging.jms.JMSQueueAdd;
import org.jboss.as.messaging.jms.JMSQueueRemove;
import org.jboss.as.messaging.jms.JMSTopicAdd;
import org.jboss.as.messaging.jms.JMSTopicRemove;
import org.jboss.as.messaging.jms.PooledConnectionFactoryAdd;
import org.jboss.as.messaging.jms.PooledConnectionFactoryRemove;

/**
 * Domain extension that integrates HornetQ.
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 */
public class MessagingExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "messaging";

    private static final PathElement CFS_PATH = PathElement.pathElement(CommonAttributes.CONNECTION_FACTORY);
    private static final PathElement JMS_QUEUE_PATH = PathElement.pathElement(CommonAttributes.JMS_QUEUE);
    private static final PathElement TOPIC_PATH = PathElement.pathElement(CommonAttributes.JMS_TOPIC);
    private static final PathElement RA_PATH = PathElement.pathElement(CommonAttributes.POOLED_CONNECTION_FACTORY);

    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(MessagingSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, MessagingSubsystemAdd.INSTANCE, MessagingSubsystemProviders.SUBSYSTEM_ADD, false);
        registration.registerOperationHandler(DESCRIBE, MessagingSubsystemDescribeHandler.INSTANCE, MessagingSubsystemProviders.SUBSYSTEM_DESCRIBE, false, OperationEntry.EntryType.PRIVATE);

        subsystem.registerXMLElementWriter(MessagingSubsystemParser.getInstance());

        final ManagementResourceRegistration queue = registration.registerSubModel(PathElement.pathElement(QUEUE), MessagingSubsystemProviders.QUEUE_RESOURCE);
        queue.registerOperationHandler(ADD, QueueAdd.INSTANCE, QueueAdd.INSTANCE, false);
        queue.registerOperationHandler(REMOVE, QueueRemove.INSTANCE, QueueRemove.INSTANCE, false);

        // Connection factories
        final ManagementResourceRegistration cfs = registration.registerSubModel(CFS_PATH, MessagingSubsystemProviders.CF);
        cfs.registerOperationHandler(ADD, ConnectionFactoryAdd.INSTANCE, MessagingSubsystemProviders.CF_ADD, false);
        cfs.registerOperationHandler(REMOVE, ConnectionFactoryRemove.INSTANCE, MessagingSubsystemProviders.CF_REMOVE, false);
        // JMS Queues
        final ManagementResourceRegistration queues = registration.registerSubModel(JMS_QUEUE_PATH, MessagingSubsystemProviders.JMS_QUEUE_RESOURCE);
        queues.registerOperationHandler(ADD, JMSQueueAdd.INSTANCE, MessagingSubsystemProviders.JMS_QUEUE_ADD, false);
        queues.registerOperationHandler(REMOVE, JMSQueueRemove.INSTANCE, MessagingSubsystemProviders.JMS_QUEUE_REMOVE, false);
        // JMS Topics
        final ManagementResourceRegistration topics = registration.registerSubModel(TOPIC_PATH, MessagingSubsystemProviders.JMS_TOPIC_RESOURCE);
        topics.registerOperationHandler(ADD, JMSTopicAdd.INSTANCE, MessagingSubsystemProviders.JMS_TOPIC_ADD, false);
        topics.registerOperationHandler(REMOVE, JMSTopicRemove.INSTANCE, MessagingSubsystemProviders.JMS_TOPIC_REMOVE, false);
        // Resource Adapter Pooled connection factories
        final ManagementResourceRegistration resourceAdapters = registration.registerSubModel(RA_PATH, MessagingSubsystemProviders.RA);
        resourceAdapters.registerOperationHandler(ADD, PooledConnectionFactoryAdd.INSTANCE, MessagingSubsystemProviders.RA_ADD, false);
        resourceAdapters.registerOperationHandler(REMOVE, PooledConnectionFactoryRemove.INSTANCE, MessagingSubsystemProviders.RA_REMOVE);
    }

    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.MESSAGING_1_0.getUriString(), MessagingSubsystemParser.getInstance());
    }

}
