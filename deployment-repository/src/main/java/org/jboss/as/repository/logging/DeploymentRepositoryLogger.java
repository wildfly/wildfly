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

package org.jboss.as.repository.logging;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "WFLYDR", length = 4)
public interface DeploymentRepositoryLogger extends BasicLogger {

    /**
     * A logger with the category of the package name.
     */
    DeploymentRepositoryLogger ROOT_LOGGER = Logger.getMessageLogger(DeploymentRepositoryLogger.class, "org.jboss.as.repository");

    /**
     * Logs an informational message indicating the content was added at the location, represented by the {@code path}
     * parameter.
     *
     * @param path the name of the path.
     */
    @LogMessage(level = INFO)
    @Message(id = 1, value = "Content added at location %s")
    void contentAdded(String path);

    @LogMessage(level = INFO)
    @Message(id = 2, value = "Content removed from location %s")
    void contentRemoved(String path);

    @LogMessage(level = WARN)
    @Message(id = 3, value = "Cannot delete temp file %s, will be deleted on exit")
    void cannotDeleteTempFile(String path);

    /**
     * Creates an exception indicating a failure to create the directory represented by the {@code path} parameter.
     *
     * @param path the path name.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 4, value = "Cannot create directory %s")
    IllegalStateException cannotCreateDirectory(String path);

    /**
     * Creates an exception indicating the inability to obtain SHA-1.
     *
     * @param cause the cause of the error.
     * @param name  the name of the class.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 5, value = "Cannot obtain SHA-1 %s")
    IllegalStateException cannotObtainSha1(@Cause Throwable cause, String name);

    /**
     * Creates an exception indicating the directory, represented by the {@code path} parameter, is not writable.
     *
     * @param path the path name.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 6, value = "Directory %s is not writable")
    IllegalStateException directoryNotWritable(String path);

    /**
     * Creates an exception indicating the path, represented by the {@code path} parameter, is not a directory.
     *
     * @param path the path name.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 7, value = "%s is not a directory")
    IllegalStateException notADirectory(String path);

    /**
     * Creates an exception indicating the variable, represented by the {@code name} parameter, is {@code null}.
     *
     * @param name the name of the variable.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 8, value = "%s is null")
    IllegalArgumentException nullVar(String name);
}
