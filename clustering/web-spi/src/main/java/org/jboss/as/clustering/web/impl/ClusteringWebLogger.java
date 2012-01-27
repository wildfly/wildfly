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

package org.jboss.as.clustering.web.impl;

import static org.jboss.logging.Logger.Level.WARN;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Date: 30.08.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
interface ClusteringWebLogger extends BasicLogger {
    String ROOT_LOGGER_CATEGORY = ClusteringWebLogger.class.getPackage().getName();

    /**
     * A logger with a category for the root clustering.
     */
    ClusteringWebLogger ROOT_LOGGER = Logger.getMessageLogger(ClusteringWebLogger.class, ROOT_LOGGER_CATEGORY);

    /**
     * Logs a warning message indicating that an exception occurred while executing the method represented by the
     * {@code methodName} parameter and is being rolled back.
     *
     * @param cause      the cause of the error.
     * @param methodName the name of the method the error occurred in.
     */
    @LogMessage(level = WARN)
    @Message(id = 10300, value = "%s: rolling back transaction with exception")
    void rollingBackTransaction(@Cause Throwable cause, String methodName);
}
