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

import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

/**
 * Log messages for WildFly batch module (message id range 20560-20599, https://community.jboss.org/wiki/LoggingIds)
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
@ValidIdRange(min = 20560, max = 20599)
public interface WildFlyBatchMessages {

    WildFlyBatchMessages MESSAGES = Messages.getBundle(WildFlyBatchMessages.class);

    /**
     * Creates an exception indicating a service was not installed.
     *
     * @param name a name for the service
     *
     * @return an {@link IllegalStateException} for the error
     */
    @Message(id = 20560, value = "%s service was not added on the deployment. Ensure the deployment has a " +
            "META-INF/batch.xml file or the META-INF/batch-jobs directory contains batch configuration files.")
    IllegalStateException serviceNotInstalled(String name);

    /**
     * Creates an exception indicating the job repository type was invalid.
     *
     * @param type the invalid type
     *
     * @return an {@link IllegalArgumentException} for the error
     */
    @Message(id = 20561, value = "Invalid job repository type '%s'.")
    IllegalArgumentException invalidJobRepositoryType(String type);
}
