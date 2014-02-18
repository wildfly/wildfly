/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.picketlink;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.picketlink.idm.config.SecurityConfigurationException;

import javax.xml.stream.XMLStreamException;

/**
 * <p>This module is using message IDs in the range 12500-12599 and 21200-21299.</p>
 *
 * <p>This file is using the subset 12500-12599 for non-logger messages.</p>
 *
 * <p>See http://community.jboss.org/docs/DOC-16810 for the full list of currently reserved JBAS message id blocks.</p>
 *
 * @author Pedro Igor
 */
@MessageBundle(projectCode = "JBAS")
public interface PicketLinkMessages {

    PicketLinkMessages MESSAGES = Messages.getBundle(PicketLinkMessages.class);

    // General Messages
    @Message(id = 12500, value = "No writer provided for element %s. Check if a writer is registered in PicketLinkSubsystemWriter.")
    IllegalStateException noModelElementWriterProvided(String modelElement);

    @Message(id = 12501, value = "Could not load module [%s].")
    RuntimeException moduleCouldNotLoad(String s, @Cause Throwable t);

    @Message(id = 12502, value = "Unexpected element [%s].")
    XMLStreamException parserUnexpectedElement(String modelName);

    @Message(id = 12503, value = "Could not load class [%s].")
    RuntimeException couldNotLoadClass(String mappingClass, @Cause Throwable e);

    // IDM Messages
    @Message(id = 12504, value = "Entities module not found [%s].")
    SecurityConfigurationException idmJpaEntityModuleNotFound(String entityModuleName);

    @Message(id = 12505, value = "Could not configure JPA store.")
    SecurityConfigurationException idmJpaStartFailed(@Cause Throwable e);

    @Message(id = 12506, value = "Could not lookup EntityManagerFactory [%s].")
    SecurityConfigurationException idmJpaEMFLookupFailed(String entityManagerFactoryJndiName);

    @Message(id = 12507, value = "No type provided for %s. You must specify a class-name or code.")
    OperationFailedException idmTypeNotProvided(String elementName);

    @Message(id = 12508, value = "You post provide at least one identity configuration.")
    OperationFailedException idmNoIdentityConfigurationProvided();

    @Message(id = 12509, value = "You post provide at least one identity store for identity configuration [%s].")
    OperationFailedException idmNoIdentityStoreProvided(String identityConfiguration);
}
