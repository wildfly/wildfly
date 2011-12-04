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

package org.jboss.as.domain.controller;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.RunningMode;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.logging.Param;

/**
 * This module is using message IDs in the range 10900-10999.
 * This file is using the subset 10975-10999 for domain controller non-logger messages.
 * See http://community.jboss.org/docs/DOC-16810 for the full list of
 * currently reserved JBAS message id blocks.
 *
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface DomainControllerMessages {

    /**
     * The messages.
     */
    DomainControllerMessages MESSAGES = Messages.getBundle(DomainControllerMessages.class);

    /**
     * Creates an exception message indicating this host is a slave and cannot accept registrations from other slaves.
     *
     * @return a message for the error.
     */
    @Message(id = 10975, value = "Registration of remote hosts is not supported on slave host controllers")
    String slaveControllerCannotAcceptOtherSlaves();

    /**
     * Creates an exception message indicating this host is in admin mode and cannot accept registrations from other slaves.
     *
     * @param runningMode the host controller's current running mode
     *
     * @return a message for the error.
     */
    @Message(id = 10976, value = "The master host controller cannot register slave host controllers as it's current running mode is '%s'")
    String adminOnlyModeCannotAcceptSlaves(RunningMode runningMode);

    /**
     * Creates an exception message indicating a host cannot register because another host of the same name is already registered.
     *
     * @param slaveName the name of the slave
     *
     * @return a message for the error.
     */
    @Message(id = 10977, value = "There is already a registered host named '%s'")
    String slaveAlreadyRegistered(String slaveName);
}
