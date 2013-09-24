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

package org.wildfly.extension.batch._private;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.Logger.Level;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

/**
 * Log messages for WildFly batch module (message id range 20500-20549, https://community.jboss.org/wiki/LoggingIds)
 */
@MessageLogger(projectCode = "JBAS")
@ValidIdRange(min = 20500, max = 20549)
public interface BatchLogger extends BasicLogger {
    /**
     * A logger with the category {@code org.wildfly.extension.batch}.
     */
    BatchLogger LOGGER = Logger.getMessageLogger(BatchLogger.class, "org.wildfly.extension.batch");

    /**
     * Logs a warning message indicating the batch configuration files were found in {@code META-INF/batch-jobs}, but
     * CDI wasn't enabled.
     */
    @LogMessage(level = Level.WARN)
    @Message(id = 20500, value = "The deployment appears to include batch configuration files in META-INF/batch-jobs but CDI was not enabled. " +
            "The batch environment will not be setup which could produce errors if attempting to execute batch jobs.")
    void cdiNotEnabled();

}
