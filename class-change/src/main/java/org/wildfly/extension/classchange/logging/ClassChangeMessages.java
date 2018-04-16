/*
 * Copyright (C) 2018 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.wildfly.extension.classchange.logging;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.tools.Diagnostic;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.modules.ModuleLoadException;
import org.jboss.vfs.VirtualFile;

@MessageLogger(projectCode = "WFLYCCHANGE", length = 4)
public interface ClassChangeMessages extends BasicLogger {

    /**
     * The logger with the category of the package.
     */
    ClassChangeMessages ROOT_LOGGER = Logger.getMessageLogger(ClassChangeMessages.class, "org.wildfly.extension.class-change");

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 1, value = "Server has been started with hot class replacement enabled. This is a developer feature only. It is not recommended for production use")
    void startedWithFakereplace();

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 2, value = "Failed to replace class file %s")
    void failedToReplaceClassFile(String name, @Cause Exception e);

    @Message(id = 3, value = "failed to read properties file %s")
    DeploymentUnitProcessingException failedToRead(VirtualFile file, @Cause IOException e);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 4, value = "Failed to compile while getting location %s")
    void failedToCompileWhileGettingLocation(JavaFileManager.Location location, @Cause ModuleLoadException e);

    @Message(id = 5, value = "Compile failed %s")
    RuntimeException compileFailed(List<Diagnostic<? extends JavaFileObject>> diagnostics);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 6, value = "Compiling changes source files %s")
    void compilingChangedClassFiles(Collection<String> changedSourceFiles);

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 7, value = "Attempting to update discovered changed classes %s and add new classes %s")
    void attemptingToReplaceClasses(List<String> modNames, List<String> addNames);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 8, value = "Cannot index class %s at %s")
    void cannotIndexClass(String className, String s, @Cause IOException e);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 9, value = "Failed to perform changed class scan")
    void failedToScan(@Cause IOException e);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 10, value = "Failed to write changed class file %s")
    void failedToWriteFile(File file, @Cause IOException e);

    @Message(id = 11, value = "The class change subsystem cannot be used in domain mode, it is only supported on standalone servers.")
    OperationFailedException domainModeNotSupported();

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 12, value = "Failed to delete class change temp dir")
    void failedToDeleteTempDir(@Cause IOException e);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 13, value = "Failed to update deployment archive %s, class changes will not persist across restarts and redeployments.")
    void failedToUpdateDeploymentArchive(String name, @Cause Exception e);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 14, value = "Failed to update web resource %s")
    void failedToupdateWebResource(String key, @Cause Exception e);

    @LogMessage(level = Logger.Level.ERROR)
    @Message(id = 15, value = "Unable to replace class changes via Fakereplace, attempting a redeployment instead")
    void attemptingRedeployment();

    @LogMessage(level = Logger.Level.INFO)
    @Message(id = 16, value = "Redeployment complete in %s ms")
    void redeploymentComplete(long l);
}
