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

import java.util.Collection;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.logging.Messages;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.logging.annotations.ValidIdRange;

/**
 * Messages for WildFly batch module (message id range 20550-20599, https://community.jboss.org/wiki/LoggingIds)
 */
@MessageLogger(projectCode = "JBAS")
@ValidIdRange(min = 20550, max = 20599)
public interface BatchMessages {

    BatchMessages MESSAGES = Messages.getBundle(BatchMessages.class);

    /**
     * Creates an exception indicating a job repository is required.
     *
     * @return an {@link OperationFailedException} for the error
     */
    @Message(id = 20550, value = "A job-repository is required.")
    OperationFailedException jobRepositoryRequired();

    /**
     * Creates an exception indicating a job repository is required.
     *
     * @param found the job repositories found
     *
     * @return an {@link OperationFailedException} for the error
     */
    @Message(id = 20551, value = "Only one job-repository is allowed. Found: %s")
    OperationFailedException multipleJobRepositoriesNotAllowed(Collection<String> found);

}
