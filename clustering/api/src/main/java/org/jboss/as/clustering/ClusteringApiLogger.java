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

package org.jboss.as.clustering;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.WARN;

import java.io.Serializable;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Date: 26.08.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface ClusteringApiLogger extends BasicLogger {

    String ROOT_LOGGER_CATEGORY = ClusteringApiLogger.class.getPackage().getName();

    /**
     * A logger with the category of the default clustering package.
     */
    ClusteringApiLogger ROOT_LOGGER = Logger.getMessageLogger(ClusteringApiLogger.class, ROOT_LOGGER_CATEGORY);

    /**
     * Logs an error message indicating an {@link InterruptedException} was issued by the caller.
     *
     * @param caller       the object that caused the exception.
     * @param categoryName the category name.
     */
    @LogMessage(level = ERROR)
    @Message(id = 10200, value = "Caught InterruptedException; Failing request by %s to lock %s")
    void caughtInterruptedException(Object caller, Serializable categoryName);


    /**
     * Logs a warning message indicating a call to {@code remoteLock} was called from itself.
     */
    @LogMessage(level = WARN)
    @Message(id = 10201, value = "Received remoteLock call from self")
    void receivedRemoteLockFromSelf();
}
