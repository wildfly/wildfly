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

import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageBundle;
import org.jboss.logging.Messages;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;
import org.jboss.vfs.VirtualFile;

import java.io.File;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface EmbeddedMessages {

    /**
     * The messages
     */
    EmbeddedMessages MESSAGES = Messages.getBundle(EmbeddedMessages.class);

    /**
     * Creates an exception indicating the file, represented by the {@code fileName} parameter, could not be mounted.
     *
     * @param cause    the cause of the error.
     * @param fileName the name of the file.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 11130, value = "Could not mount file '%s'")
    RuntimeException cannotMountFile(@Cause Throwable cause, String fileName);

    /**
     * Creates an exception indicating the contents of the file could not be read.
     *
     * @param cause the cause of the error.
     * @param file  the file that could not be read.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 11131, value = "Could not read contents of %s")
    RuntimeException cannotReadContent(@Cause Throwable cause, VirtualFile file);

    /**
     * Creates an exception indicating one or more exclusion values must be specified.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11132, value = "One or more exclusion values must be specified")
    IllegalArgumentException exclusionValuesRequired();

    /**
     * A message indicating a failure to load the specified log module.
     *
     * @param moduleId the module id.
     *
     * @return the message.
     */
    @Message(id = 11133, value = "WARNING: Failed to load the specified logmodule %s")
    String failedToLoadLogModule(ModuleIdentifier moduleId);

    /**
     * Creates an exception indicating the JBoss hom directory is invalid.
     *
     * @param dir the invalid directory.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 11134, value = "Invalid JBoss home directory: %s")
    IllegalStateException invalidJbossHome(File dir);

    /**
     * Creates an exception indicating the module path is invalid.
     *
     * @param file the invalid file.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11135, value = "Invalid module path: %s")
    IllegalArgumentException invalidModulePath(File file);

    /**
     * Creates an exception indicating the module, represented by the {@code moduleName} parameter, was not a valid
     * type of {@code File[]}, {@code File}, {@code String[]} or {@code String}.
     *
     * @param moduleName the name of the module.
     * @param type       the type the module actually is.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 11136, value = "%s was not of type File[], File, String[] or String, but of type %s")
    RuntimeException invalidModuleType(String moduleName, Class<?> type);

    /**
     * Creates an exception indicating there was an error in the module loader.
     *
     * @param cause        the cause of the error.
     * @param msg          the error message.
     * @param moduleLoader the module loader that had the error.
     *
     * @return a {@link RuntimeException} for the error.
     */
    @Message(id = 11137, value = "%s in %s")
    RuntimeException moduleLoaderError(@Cause Throwable cause, String msg, ModuleLoader moduleLoader);

    /**
     * Creates an exception indicating the variable, represented by the {@code name} parameter, is {@code null}.
     *
     * @param name the name of the variable.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 11138, value = "%s is null")
    IllegalArgumentException nullVar(String name);

    /**
     * Creates an exception indicating the system property could not be found.
     *
     * @param key they key to the system property.
     *
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 11139, value = "Cannot find system property: %s")
    IllegalStateException systemPropertyNotFound(String key);
}
