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

package org.jboss.as.osgi.plugin;

import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

import static org.jboss.logging.Logger.Level.WARN;

/**
 * Date: 27.06.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
interface OSGiPluginLogger {
    /**
     * The root logger.
     */
    OSGiPluginLogger ROOT_LOGGER = Logger.getMessageLogger(OSGiPluginLogger.class, OSGiPluginLogger.class.getPackage().getName());

    /**
     * Logs a warning message indicating the client could not be undeployed.
     *
     * @param cause the cause of the error.
     * @param name  the name.
     */
    @LogMessage(level = WARN)
    @Message(id = 11900, value = "Cannot undeploy: %s")
    void cannotUndeploy(@Cause Throwable cause, String name);
}
