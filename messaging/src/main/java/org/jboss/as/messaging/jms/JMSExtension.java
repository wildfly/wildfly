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

package org.jboss.as.messaging.jms;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.controller.registry.OperationEntry;

/**
 * The JMS extension.
 *
 * @author Emanuel Muckenhuber
 */
public class JMSExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "jms";

    private static final PathElement CFS_PATH = PathElement.pathElement(CommonAttributes.CONNECTION_FACTORY);
    private static final PathElement QUEUE_PATH = PathElement.pathElement(CommonAttributes.QUEUE);
    private static final PathElement TOPIC_PATH = PathElement.pathElement(CommonAttributes.TOPIC);

    private static final JMSSubsystemParser parsers = JMSSubsystemParser.getInstance();

    /** {@inheritDoc} */
    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ModelNodeRegistration registration = subsystem.registerSubsystemModel(JMSSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, JMSSubsystemAdd.INSTANCE, JMSSubsystemProviders.SUBSYSTEM_ADD, false);
        registration.registerOperationHandler(DESCRIBE, JMSSubsystemDescribeHandler.INSTANCE, JMSSubsystemProviders.SUBSYSTEM_DESCRIBE, false, OperationEntry.EntryType.PRIVATE);
        subsystem.registerXMLElementWriter(parsers);
        // Connection factories
        final ModelNodeRegistration cfs = registration.registerSubModel(CFS_PATH, JMSSubsystemProviders.CF);
        cfs.registerOperationHandler(ADD, ConnectionFactoryAdd.INSTANCE, JMSSubsystemProviders.CF_ADD, false);
        cfs.registerOperationHandler(REMOVE, ConnectionFactoryRemove.INSTANCE, JMSSubsystemProviders.CF_REMOVE, false);
        // Queues
        final ModelNodeRegistration queues = registration.registerSubModel(QUEUE_PATH, JMSSubsystemProviders.JMS_QUEUE);
        queues.registerOperationHandler(ADD, JMSQueueAdd.INSTANCE, JMSSubsystemProviders.JMS_QUEUE_ADD, false);
        queues.registerOperationHandler(REMOVE, JMSQueueRemove.INSTANCE, JMSSubsystemProviders.JMS_QUEUE_REMOVE, false);
        // Topics
        final ModelNodeRegistration topics = registration.registerSubModel(TOPIC_PATH, JMSSubsystemProviders.JMS_TOPIC);
        topics.registerOperationHandler(ADD, JMSTopicAdd.INSTANCE, JMSSubsystemProviders.JMS_TOPIC_ADD, false);
        topics.registerOperationHandler(REMOVE, JMSTopicRemove.INSTANCE, JMSSubsystemProviders.JMS_TOPIC_REMOVE, false);
    }

    /** {@inheritDoc} */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.CURRENT.getUriString(), parsers);
    }

}
