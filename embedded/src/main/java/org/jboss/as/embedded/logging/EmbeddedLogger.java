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

package org.jboss.as.embedded.logging;

import org.jboss.as.embedded.ServerStartException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.modules.ModuleLoader;
import org.jboss.vfs.VirtualFile;

import java.io.File;
import java.lang.reflect.Method;

import static org.jboss.logging.Logger.Level.WARN;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "WFLYEMB", length = 4)
public interface EmbeddedLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    EmbeddedLogger ROOT_LOGGER = Logger.getMessageLogger(EmbeddedLogger.class, "org.jboss.as.embedded");

    /**
     * Logs a warning message indicating the file handle, represented by the {@code file} parameter, could not be
     * closed.
     *
     * @param cause the cause of the error.
     * @param file  the file.
     */
    @LogMessage(level = WARN)
    @Message(id = 1, value = "Could not close handle to mounted %s")
    void cannotCloseFile(@Cause Throwable cause, VirtualFile file);

    /**
     * Logs a warning message indicating the class file, represented by the {@code file} parameter, could not be loaded.
     *
     * @param cause the cause of the error.
     * @param file  the file that could not be loaded.
     */
    @LogMessage(level = WARN)
    @Message(id = 2, value = "Could not load class file %s")
    void cannotLoadClassFile(@Cause Throwable cause, VirtualFile file);

    /**
     * Logs a warning message indicating there was an exception closing the file.
     *
     * @param cause the cause of the error.
     * @param file  the file that failed to close.
     */
    @LogMessage(level = WARN)
    @Message(id = 3, value = "Exception closing file %s")
    void errorClosingFile(@Cause Throwable cause, VirtualFile file);

    /**
     * Logs a warning message indicating there was a failure to undeploy the file.
     *
     * @param cause the cause of the error.
     * @param file  the file that failed to undeploy.
     */
    @LogMessage(level = WARN)
    @Message(id = 4, value = "Failed to undeploy %s")
    void failedToUndeploy(@Cause Throwable cause, File file);

    /**
     * Logs a warning message indicating the file on the ClassPath could not be found.
     *
     * @param file the file that could not be found.
     */
    @LogMessage(level = WARN)
    @Message(id = 5, value = "File on ClassPath could not be found: %s")
    void fileNotFound(VirtualFile file);

    /**
     * Logs a warning message indicating an unknown file type was encountered and is being skipped.
     *
     * @param file the file.
     */
    @LogMessage(level = WARN)
    @Message(id = 6, value = "Encountered unknown file type, skipping: %s")
    void skippingUnknownFileType(VirtualFile file);


    /**
     * Creates an exception indicating the file, represented by the {@code fileName} parameter, could not be mounted.
     */
    @Message(id = 7, value = "Could not mount file '%s'")
    RuntimeException cannotMountFile(@Cause Throwable cause, String fileName);

    /**
     * Creates an exception indicating the contents of the file could not be read.
     */
    @Message(id = 8, value = "Could not read contents of %s")
    RuntimeException cannotReadContent(@Cause Throwable cause, VirtualFile file);

    /**
     */
    @Message(id = 9, value = "One or more exclusion values must be specified")
    IllegalArgumentException exclusionValuesRequired();

    //@Message(id = 10, value = "WARNING: Failed to load the specified logmodule %s")
    //String failedToLoadLogModule(ModuleIdentifier moduleId);

    /**
     */
    @Message(id = 11, value = "Invalid JBoss home directory: %s")
    IllegalStateException invalidJBossHome(String jbossHome);

    /**
     * Creates an exception indicating the module path is invalid.
     */
    @Message(id = 12, value = "Invalid module path: %s")
    IllegalArgumentException invalidModulePath(String file);

    /**
     * Creates an exception indicating the module, represented by the {@code moduleName} parameter, was not a valid
     * type of {@code File[]}, {@code File}, {@code String[]} or {@code String}.
     */
    @Message(id = 13, value = "%s was not of type File[], File, String[] or String, but of type %s")
    RuntimeException invalidModuleType(String moduleName, Class<?> type);

    /**
     * Creates an exception indicating there was an error in the module loader.
     */
    @Message(id = 14, value = "Cannot load module %s from: %s")
    RuntimeException moduleLoaderError(@Cause Throwable cause, String msg, ModuleLoader moduleLoader);

    /**
     * Creates an exception indicating the variable, represented by the {@code name} parameter, is {@code null}.
     */
    @Message(id = 15, value = "%s is null")
    IllegalArgumentException nullVar(String name);

    /**
     * Creates an exception indicating the system property could not be found.
     */
    @Message(id = 16, value = "Cannot find system property: %s")
    IllegalStateException systemPropertyNotFound(String key);

    @Message(id = 17, value = "Cannot load embedded server factory: %s")
    IllegalStateException cannotLoadEmbeddedServerFactory(@Cause ClassNotFoundException cause, String className);

    @Message(id = 18, value = "Cannot get reflective method '%s' for: %s")
    IllegalStateException cannotGetReflectiveMethod(@Cause NoSuchMethodException cause, String method, String className);

    @Message(id = 19, value = "Cannot create standalone server using factory: %s")
    IllegalStateException cannotCreateStandaloneServer(@Cause Throwable cause, Method createMethod);

    @Message(id = 20, value = "Cannot setup embedded server")
    IllegalStateException cannotSetupEmbeddedServer(@Cause Throwable cause);

    @Message(id = 21, value = "Cannot start embedded server")
    ServerStartException cannotStartEmbeddedServer(@Cause Throwable cause);

    @Message(id = 22, value = "Cannot invoke '%s' on standalone server")
    IllegalStateException cannotInvokeStandaloneServer(@Cause Throwable cause, String methodName);
}
