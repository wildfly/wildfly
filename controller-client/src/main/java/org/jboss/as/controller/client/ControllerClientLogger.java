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

package org.jboss.as.controller.client;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.Logger;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.WARN;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface ControllerClientLogger extends BasicLogger {

    /**
     * A logger with the default package name.
     */
    ControllerClientLogger ROOT_LOGGER = Logger.getMessageLogger(ControllerClientLogger.class, ControllerClientLogger.class.getPackage().getName());

    // 10600 - 10619 {@see ControllerClientMessages}

    /**
     * Logs a warn message indicating that a controller client wasn't closed properly.
     *
     * @param allocationStackTrace the allocation stack trace
     */
    @LogMessage(level = WARN)
    @Message(id = 10600, value = "Closing leaked controller client")
    void leakedControllerClient(@Cause Throwable allocationStackTrace);

    /**
     * Logs a warnning message indicating a temp file could not be deleted.
     *
     * @param name temp filename
     */
    @LogMessage(level = WARN)
    @Message(id = 10601, value = "Cannot delete temp file %s, will be deleted on exit")
    void cannotDeleteTempFile(String name);

}
