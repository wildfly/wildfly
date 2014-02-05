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

package org.wildfly.jberet._private;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "WFLYBAT", length = 4)
public interface WildFlyBatchLogger extends BasicLogger {

    WildFlyBatchLogger LOGGER = Logger.getMessageLogger(WildFlyBatchLogger.class, "org.wildfly.jberet");

    /**
     * Creates an exception indicating a service was not installed.
     *
     * @param name a name for the service
     *
     * @return an {@link IllegalStateException} for the error
     */
    @Message(id = 1, value = "%s service was not added on the deployment. Ensure the deployment has a " +
            "META-INF/batch.xml file or the META-INF/batch-jobs directory contains batch configuration files.")
    IllegalStateException serviceNotInstalled(String name);

    /**
     * Creates an exception indicating the job repository type was invalid.
     *
     * @param type the invalid type
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 2, value = "Invalid job repository type '%s'.")
    IllegalArgumentException invalidJobRepositoryType(String type);

    /**
     * Creates an exception indicating the batch environment has not been configured or has been removed.
     *
     * @return an {@link IllegalStateException} for the error
     */
    @Message(id = 3, value = "The batch environment was not configured or has been removed.")
    IllegalStateException invalidBatchEnvironment();
}
