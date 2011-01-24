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

import org.jboss.as.controller.NewExtension;
import org.jboss.as.controller.NewExtensionContext;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.messaging.jms.NewJMSExtension;

/**
 * @author Emanuel Muckenhuber
 */
public class NewMessagingExtension implements NewExtension {

    public static final String SUBSYSTEM_NAME = "messaging";

    private final NewJMSExtension JMS_EXTENSION = new NewJMSExtension();

    /** {@inheritDoc} */
    public void initialize(NewExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME);
        final ModelNodeRegistration registration = subsystem.registerSubsystemModel(NewMessagingSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, NewMessagingSubsystemAdd.INSTANCE, NewMessagingSubsystemProviders.SUBSYSTEM_ADD, false);
        // Also register JMS subsystem
        JMS_EXTENSION.initialize(context);
    }

    /** {@inheritDoc} */
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Namespace.MESSAGING_1_0.getUriString(), NewMessagingSubsystemParser.getInstance(), NewMessagingSubsystemParser.getInstance());
        // Also register JMS subsystem
        JMS_EXTENSION.initializeParsers(context);
    }

}
