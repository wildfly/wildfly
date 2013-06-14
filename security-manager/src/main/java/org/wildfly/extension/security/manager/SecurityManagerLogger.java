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

package org.wildfly.extension.security.manager;

import static org.jboss.logging.Logger.Level.INFO;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * The security manager subsystem is using message IDs in the range 17700-17799. This file is using the subset
 * 17700-17749 for logger messages.
 * </p>
 * See http://http://community.jboss.org/wiki/LoggingIds for the full list of currently reserved JBAS message id blocks.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface SecurityManagerLogger extends BasicLogger {

    SecurityManagerLogger ROOT_LOGGER = Logger.getMessageLogger(SecurityManagerLogger.class, SecurityManagerLogger.class.getPackage().getName());

    @LogMessage(level = INFO)
    @Message(id = 17700, value = "Activating Security Manager Subsystem")
    void activatingSubsystem();

    @LogMessage(level = INFO)
    @Message(id = 17701, value = "Instaling the WildFlySecurityManager")
    void installingWildFlySecurityManager();

}
