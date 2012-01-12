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

package org.jboss.as.server.deployment.repository.impl;

import org.jboss.as.server.deployment.repository.api.MountType;
import org.jboss.logging.Cause;
import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.msc.service.StartException;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
interface DeploymentRepositoryMessages {

    /**
     * The messages
     */
    DeploymentRepositoryMessages MESSAGES = Messages.getBundle(DeploymentRepositoryMessages.class);

    /**
     * Creates an exception indicating the a failure to create the directory represented by the {@code path} parameter.
     *
     * @param path the path name.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14920, value = "Cannot create directory %s")
    IllegalStateException cannotCreateDirectory(String path);

    /**
     * Creates an exception indicating the inability to obtain SHA-1.
     *
     * @param cause the cause of the error.
     * @param name  the name of the class.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14921, value = "Cannot obtain SHA-1 %s")
    IllegalStateException cannotObtainSha1(@Cause Throwable cause, String name);

    /**
     * Creates an exception indicating the directory, represented by the {@code path} parameter, is not writable.
     *
     * @param path the path name.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14922, value = "Directory %s is not writable")
    IllegalStateException directoryNotWritable(String path);

    /**
     * Creates an exception indicating a failure to create a temp file provider.
     *
     * @return a {@link StartException} for the error.
     */
    @Message(id = 14923, value = "Failed to create temp file provider")
    StartException failedCreatingTempProvider();

    /**
     * Creates an exception indicating the path, represented by the {@code path} parameter, is not a directory.
     *
     * @param path the path name.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 14924, value = "%s is not a directory")
    IllegalStateException notADirectory(String path);

    /**
     * Creates an exception indicating the variable, represented by the {@code name} parameter, is {@code null}.
     *
     * @param name the name of the variable.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 14925, value = "%s is null")
    IllegalArgumentException nullVar(String name);

    @Message(id = 14926, value = "Unknown mount type %s")
    IllegalArgumentException unknownMountType(MountType mountType);
}
