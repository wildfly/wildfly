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

package org.jboss.as.server.deployment.scanner;

import org.jboss.logging.Message;
import org.jboss.logging.MessageBundle;
import org.jboss.logging.Messages;

import java.io.File;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageBundle(projectCode = "JBAS")
public interface DeploymentScannerMessages {

    /**
     * The messages
     */
    DeploymentScannerMessages MESSAGES = Messages.getBundle(DeploymentScannerMessages.class);

    /**
     * A message indicating the subsystem cannot be removed while there are still scanners configured.
     *
     * @return the message.
     */
    @Message(id = 15050, value = "Cannot remove subsystem while it still has scanners configured. Remove all scanners first.")
    String cannotRemoveSubsystem();

    /**
     * A message indicating the deployment content, represented by the {@code file} parameter, appears to be incomplete
     * and is not progressing toward completion.
     *
     * @param file   the content.
     * @param suffix the suffix.
     *
     * @return the message.
     */
    @Message(id = 15051, value = "Deployment content %s appears to be incomplete and is not progressing toward completion. This" +
            " content cannot be auto-deployed.%s")
    String deploymentContentIncomplete(File file, String suffix);

    /**
     * A message indicating the deployment operation was not received with the timeout period.
     *
     * @param timeout the timeout period.
     *
     * @return the message.
     */
    @Message(id = 15052, value = "Did not receive a response to the deployment operation within the allowed timeout period " +
            "[%d seconds]. Check the server configuration file and the server logs to find more about the status of " +
            "the deployment.")
    String deploymentTimeout(long timeout);

    /**
     * Creates an exception indicating the directory, represented by the {@code path} parameter, does not exist.
     *
     * @param path the path of the directory.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 15053, value = "%s does not exist")
    IllegalArgumentException directoryDoesNotExist(String path);

    /**
     * Creates an exception indicating the directory, represented by the {@code path} parameter, is not writable.
     *
     * @param path the path of the directory.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 15054, value = "%s is not writable")
    IllegalArgumentException directoryNotWritable(String path);

    /**
     * A message indicating the file, represented by the {@code fileName} parameter, cannot be scanned because it does
     * not begin with a ZIP file format local file header signature.
     *
     * @param fileName the name of the invalid file.
     *
     * @return the messages.
     */
    @Message(id = 15055, value = "File %s cannot be scanned because it does not begin with a ZIP file format local file header signature")
    String invalidZipFileFormat(String fileName);

    /**
     * A message indicating the file, represented by the {@code fileName} parameter, cannot be scanned because it uses
     * the currently unsupported ZIP64 format.
     *
     * @param fileName the name of the invalid file.
     *
     * @return the messages.
     */
    @Message(id = 15056, value = "File %s cannot be scanned because it uses the currently unsupported ZIP64 format")
    String invalidZip64FileFormat(String fileName);

    /**
     * Creates an exception indicating the {@code path} is not a directory.
     *
     * @param path the path name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 15057, value = "%s is not a directory")
    IllegalArgumentException notADirectory(String path);

    /**
     * Creates an exception indicating the variable, represented by the {@code name} parameter, is {@code null}.
     *
     * @param name the name of the {@code null} variable.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 15058, value = "%s is null")
    IllegalArgumentException nullVar(String name);

    /**
     * A message indicating a previous version of this content was deployed and remains deployed.
     *
     * @return the message.
     */
    @Message(id = Message.NONE, value = " A previous version of this content was deployed and remains deployed.")
    String previousContentDeployed();

    /**
     * A message indicating the scanner has not been configured.
     *
     * @return the message.
     */
    @Message(id = 15059, value = "scanner not configured")
    String scannerNotConfigured();

    /**
     * A message indicating the file was configured for auto-deploy but could not be safely auto-deployed.
     *
     * @param errorMsg the error message.
     * @param fileName the file name.
     * @param marker   the marker.
     *
     * @return the message.
     */
    @Message(id = 15060, value = "File %2$s was configured for auto-deploy but could not be safely auto-deployed. The reason the file " +
            "could not be auto-deployed was: %1$s.  To enable deployment of this file create a file called %2$s%3$s")
    String unsafeAutoDeploy(String errorMsg, String fileName, String marker);
}
