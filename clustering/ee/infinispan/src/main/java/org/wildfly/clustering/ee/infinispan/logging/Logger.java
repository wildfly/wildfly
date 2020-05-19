/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ee.infinispan.logging;

import static org.jboss.logging.Logger.Level.INFO;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Logger for the org.wildfly.clustering.ee.infinispan module.
 * @author Paul Ferraro
 */
@MessageLogger(projectCode = "WFLYCLEEINF", length = 4)
public interface Logger extends BasicLogger {
    String ROOT_LOGGER_CATEGORY = "org.wildfly.clustering.ee.infinispan";

    Logger ROOT_LOGGER = org.jboss.logging.Logger.getMessageLogger(Logger.class, ROOT_LOGGER_CATEGORY);

    @LogMessage(level = INFO)
    @Message(id = 1, value = "Failed to cancel %s on primary owner.")
    void failedToCancel(@Cause Throwable cause, Object id);

    @LogMessage(level = INFO)
    @Message(id = 2, value = "Failed to schedule %s on primary owner.")
    void failedToSchedule(@Cause Throwable cause, Object id);
}
