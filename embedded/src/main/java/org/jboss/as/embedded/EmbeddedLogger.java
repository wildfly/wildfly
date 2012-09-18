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

package org.jboss.as.embedded;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.vfs.VirtualFile;

import java.io.File;

import static org.jboss.logging.Logger.Level.WARN;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface EmbeddedLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    EmbeddedLogger ROOT_LOGGER = Logger.getMessageLogger(EmbeddedLogger.class, EmbeddedLogger.class.getPackage().getName());

    /**
     * Logs a warning message indicating the file handle, represented by the {@code file} parameter, could not be
     * closed.
     *
     * @param cause the cause of the error.
     * @param file  the file.
     */
    @LogMessage(level = WARN)
    @Message(id = 11100, value = "Could not close handle to mounted %s")
    void cannotCloseFile(@Cause Throwable cause, VirtualFile file);

    /**
     * Logs a warning message indicating the class file, represented by the {@code file} parameter, could not be loaded.
     *
     * @param cause the cause of the error.
     * @param file  the file that could not be loaded.
     */
    @LogMessage(level = WARN)
    @Message(id = 11101, value = "Could not load class file %s")
    void cannotLoadClassFile(@Cause Throwable cause, VirtualFile file);

    /**
     * Logs a warning message indicating there was an exception closing the file.
     *
     * @param cause the cause of the error.
     * @param file  the file that failed to close.
     */
    @LogMessage(level = WARN)
    @Message(id = 11102, value = "Exception closing file %s")
    void errorClosingFile(@Cause Throwable cause, VirtualFile file);

    /**
     * Logs a warning message indicating there was a failure to undeploy the file.
     *
     * @param cause the cause of the error.
     * @param file  the file that failed to undeploy.
     */
    @LogMessage(level = WARN)
    @Message(id = 11103, value = "Failed to undeploy %s")
    void failedToUndeploy(@Cause Throwable cause, File file);

    /**
     * Logs a warning message indicating the file on the ClassPath could not be found.
     *
     * @param file the file that could not be found.
     */
    @LogMessage(level = WARN)
    @Message(id = 11104, value = "File on ClassPath could not be found: %s")
    void fileNotFound(VirtualFile file);

    /**
     * Logs a warning message indicating an unknown file type was encountered and is being skipped.
     *
     * @param file the file.
     */
    @LogMessage(level = WARN)
    @Message(id = 11105, value = "Encountered unknown file type, skipping: %s")
    void skippingUnknownFileType(VirtualFile file);
}
