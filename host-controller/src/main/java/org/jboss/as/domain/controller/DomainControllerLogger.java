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

import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * This module is using message IDs in the range 10900-10999.
 * This file is using the subset 10950-10974 for domain controller logger messages.
 * See http://community.jboss.org/docs/DOC-16810 for the full list of
 * currently reserved JBAS message id blocks.
 *
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface DomainControllerLogger extends BasicLogger {

    /**
     * A logger with the category of the package.
     */
    DomainControllerLogger ROOT_LOGGER = Logger.getMessageLogger(DomainControllerLogger.class, DomainControllerLogger.class.getPackage().getName());

    /**
     * A logger with the category of {@code org.jboss.as.controller}.
     */
    DomainControllerLogger CONTROLLER_LOGGER = Logger.getMessageLogger(DomainControllerLogger.class, "org.jboss.as.controller");

    /**
     * A logger with the category of {@code org.jboss.as.deployment}.
     */
    DomainControllerLogger DEPLOYMENT_LOGGER = Logger.getMessageLogger(DomainControllerLogger.class, "org.jboss.as.deployment");

    /**
     * A logger with the category of {@code org.jboss.as.domain.deployment}.
     */
    DomainControllerLogger DOMAIN_DEPLOYMENT_LOGGER = Logger.getMessageLogger(DomainControllerLogger.class, "org.jboss.as.domain.deployment");

    /**
     * A logger with the category of {@code org.jboss.as.host.controller}.
     */
    DomainControllerLogger HOST_CONTROLLER_LOGGER = Logger.getMessageLogger(DomainControllerLogger.class, "org.jboss.as.host.controller");

    @LogMessage(level = Level.WARN)
    @Message(id = 11500, value = "Ignoring 'include' child of 'socket-binding-group' %s")
    void warnIgnoringSocketBindingGroupIgnore(Location location);


}
