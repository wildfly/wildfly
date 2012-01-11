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

package org.jboss.as.configadmin;

import org.jboss.as.configadmin.service.ConfigAdminListener;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;

/**
 /**
 * This module is using message IDs in the range 16200-16299.
 * This file is using the subset 16200-16249 for logger messages.
 * See http://http://community.jboss.org/wiki/LoggingIds for the full list of
 * currently reserved JBAS message id blocks.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Thomas.Diesler@jboss.com
 */
@MessageLogger(projectCode = "JBAS")
public interface ConfigAdminLogger extends BasicLogger {

    ConfigAdminLogger ROOT_LOGGER = Logger.getMessageLogger(ConfigAdminLogger.class, ConfigAdminLogger.class.getPackage().getName());

    @LogMessage(level = INFO)
    @Message(id = 16200, value = "Activating ConfigAdmin Subsystem")
    void activatingSubsystem();

    @LogMessage(level = ERROR)
    @Message(id = 16201, value = "Error in configuration listener: %s")
    void configurationListenerError(@Cause Throwable cause, ConfigAdminListener listener);

    @LogMessage(level = ERROR)
    @Message(id = 16202, value = "Cannot add configuration for pid: %s")
    void cannotAddConfiguration(@Cause Throwable cause, String pid);

    @LogMessage(level = ERROR)
    @Message(id = 16203, value = "Cannot add configuration for pid: %s -> %s")
    void cannotAddConfiguration(String pid, ModelNode node);

    @LogMessage(level = ERROR)
    @Message(id = 16204, value = "Cannot remove configuration for pid: %s")
    void cannotRemoveConfiguration(@Cause Throwable cause, String pid);

    @LogMessage(level = ERROR)
    @Message(id = 16205, value = "Cannot remove configuration for pid: %s -> %s")
    void cannotRemoveConfiguration(String pid, ModelNode node);
}
