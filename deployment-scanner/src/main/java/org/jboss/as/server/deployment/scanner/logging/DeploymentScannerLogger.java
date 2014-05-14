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

package org.jboss.as.server.deployment.scanner.logging;

import java.io.File;
import java.util.Set;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "WFLYDS", length = 4)
public interface DeploymentScannerLogger extends BasicLogger {

    /**
     * A logger with the category of the package name.
     */
    DeploymentScannerLogger ROOT_LOGGER = Logger.getMessageLogger(DeploymentScannerLogger.class, "org.jboss.as.server.deployment.scanner");

    /**
     * Logs a warning message indicating the extraneous deployment marker file could not be removed.
     *
     * @param file the file that could not be removed.
     */
    @LogMessage(level = WARN)
    @Message(id = 1, value = "Cannot delete deployment progress marker file %s")
    void cannotDeleteDeploymentProgressMarker(File file);

    /**
     * Logs a warning message indicating the extraneous deployment marker file could not be removed.
     *
     * @param fileName the file that could not be removed.
     */
    @LogMessage(level = WARN)
    @Message(id = 2, value = "Cannot remove extraneous deployment marker file %s")
    void cannotRemoveDeploymentMarker(String fileName);

    /**
     * Logs a warning message indicating the extraneous deployment marker file could not be removed.
     *
     * @param file the file that could not be removed.
     */
    @LogMessage(level = WARN)
    void cannotRemoveDeploymentMarker(File file);

    /**
     * Logs a warning message indicating the deployment, represented by the {@code deploymentName} parameter, requested
     * is not present.
     *
     * @param deploymentName the name of the deployment.
     */
    @LogMessage(level = WARN)
    @Message(id = 3, value = "Deployment of '%s' requested, but the deployment is not present")
    void deploymentNotFound(String deploymentName);

    /**
     * Logs a warning message indicating a deployment was triggered.
     *
     * @param fileName the file name.
     * @param marker   the marker.
     */
    @LogMessage(level = INFO)
    @Message(id = 4, value = "Found %1$s in deployment directory. To trigger deployment create a file called %1$s%2$s")
    void deploymentTriggered(String fileName, String marker);

    /**
     * Logs an error message indicating an exception was caught writing a deployment marker file.
     *
     * @param cause      the cause of the error.
     * @param markerFile the marker file.
     */
    @LogMessage(level = ERROR)
    @Message(id = 5, value = "Caught exception writing deployment marker file %s")
    void errorWritingDeploymentMarker(@Cause Throwable cause, String markerFile);

    /**
     * Logs a warning message indicating the behaviour is not possible when auto-deployment of exploded content is
     * enabled.
     *
     * @param marker      the marker.
     * @param elementName the element name.
     */
    @LogMessage(level = WARN)
    @Message(id = 6, value = "Reliable deployment behaviour is not possible when auto-deployment of exploded content is enabled " +
            "(i.e. deployment without use of \"%s\"' marker files). Configuration of auto-deployment of exploded content " +
            "is not recommended in any situation where reliability is desired. Configuring the deployment " +
            "scanner's %s setting to \"false\" is recommended.")
    void explodedAutoDeploymentContentWarning(String marker, String elementName);

    /**
     * Logs a warning message indicating the deployment scanner found that content for an exploded deployment,
     * represented by the {@code fileName} parameter, has been deleted, but marked for auto-deploy/undeploy for
     * exploded deployments is not enabled.
     *
     * @param fileName the file name of the deployment.
     * @param marker   the marker.
     */
    @LogMessage(level = WARN)
    @Message(id = 7, value = "The deployment scanner found that the content for exploded deployment %1$s has been " +
            "deleted, but auto-deploy/undeploy for exploded deployments is not enabled and the %1$s%2$s " +
            "marker file for this deployment has not been removed. As a result, the deployment is " +
            "not being undeployed, but resources needed by the deployment may have been deleted " +
            "and application errors may occur. Deleting the %1$s%2$s marker file to trigger undeploy is recommended.")
    void explodedDeploymentContentDeleted(String fileName, String marker);

    /**
     * Logs an error message indicating a failure checking whether the zip file, represented by the {@code fileName}
     * parameter, was a complete zip.
     *
     * @param cause    the cause of the error.
     * @param fileName the file name.
     */
    @LogMessage(level = ERROR)
    @Message(id = 8, value = "Failed checking whether %s was a complete zip")
    void failedCheckingZipFile(@Cause Throwable cause, String fileName);

    /**
     * Logs an error message indicating the file system deployment service failed.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 9, value = "File system deployment service failed")
    void fileSystemDeploymentFailed(@Cause Throwable cause);

    /**
     * Logs an informational message indicating scan found incompletely copied file content for deployment.
     *
     * @param name the name of the deployment.
     */
    @LogMessage(level = INFO)
    @Message(id = 10, value = "Scan found incompletely copied file content for deployment %s. Deployment changes will not be " +
            "processed until all content is complete.")
    void incompleteContent(String name);

    /**
     * Logs a warning message indicating the deployment scanner found a directory, represented by the {@code fileName}
     * parameter, that was not inside a directory with a proper ending name.
     *
     * @param fileName      the invalid deployment file name.
     * @param deploymentDir the deployment directory.
     */
    @LogMessage(level = ERROR)
    @Message(id = 11, value = "The deployment scanner found a directory named %1$s that was not inside a directory whose " +
            "name ends with .ear, .jar, .rar, .sar or .war. This is likely the result of unzipping an " +
            "archive directly inside the %2$s directory, which is a user error. " +
            "The %1$s directory will not be scanned for deployments, but it is possible that the scanner may " +
            "find other files from the unzipped archive and attempt to deploy them, leading to errors.")
    void invalidExplodedDeploymentDirectory(String fileName, String deploymentDir);

    /**
     * Logs an error message indicating the scan of the file, represented by the {@code fileName} parameter, threw an
     * exception.
     *
     * @param cause    the cause of the error.
     * @param fileName the name of the file scanned that threw an exception.
     */
    @LogMessage(level = ERROR)
    @Message(id = 12, value = "Scan of %s threw Exception")
    void scanException(@Cause Throwable cause, String fileName);

    /**
     * Logs an informational message indicating the {@code className} started for {@code deploymentPath}.
     *
     * @param className      the class name.
     * @param deploymentPath the deployment path.
     */
    @LogMessage(level = INFO)
    @Message(id = 13, value = "Started %s for directory %s")
    void started(String className, String deploymentPath);

    /**
     * Logs a warning message indicating a scan found content configured for auto-deployed that could not be safely
     * auto-deployed.
     *
     * @param marker1  the first marker option.
     * @param marker2  the second marker option.
     * @param problems the problems.
     */
    @LogMessage(level = WARN)
    @Message(id = 14, value = "Scan found content configured for auto-deploy that could not be safely auto-deployed. See details above. " +
            "Deployment changes will not be processed until all problematic content is either removed or whether to " +
            "deploy the content or not is indicated via a %s or %s marker file. Problematic deployments are %s")
    void unsafeAutoDeploy(String marker1, String marker2, Set<String> problems);

    /**
     * Logs an info level message indicating that a failed deployment {@code deploymentName} is being re-attempted.
     * @param deploymentName The name of the failed deployment.
     */
    @LogMessage(level = INFO)
    @Message(id = 15, value = "Re-attempting failed deployment %s")
    void reattemptingFailedDeployment(String deploymentName);

    /**
     * Logs an error message indicating a failure checking whether the xml file, represented by the {@code fileName}
     * parameter, was a complete xml file.
     *
     * @param cause    the cause of the error.
     * @param fileName the file name.
     */
    @LogMessage(level = ERROR)
    @Message(id = 16, value = "Failed checking whether %s was a complete XML")
    void failedCheckingXMLFile(@Cause Throwable cause, String fileName);

    @LogMessage(level = ERROR)
    @Message(id = 17, value = "Initial deployment scan failed")
    void initialScanFailed(@Cause Throwable t);

    @LogMessage(level = INFO)
    @Message(id = 18, value = "Deployment %s was previously deployed by this scanner but has been undeployed by " +
            "another management tool. Marker file %s is being added to record this fact.")
    void scannerDeploymentUndeployedButNotByScanner(String deploymentName, File marker);

    @LogMessage(level = INFO)
    @Message(id = 19, value = "Deployment %s was previously deployed by this scanner but has been removed from the " +
            "server deployment list by another management tool. Marker file %s is being added to record this fact.")
    void scannerDeploymentRemovedButNotByScanner(String deploymentName, File marker);

    /**
     * A message indicating the subsystem cannot be removed while there are still scanners configured.
     *
     * @return the message.
     */
    @Message(id = 20, value = "Cannot remove subsystem while it still has scanners configured. Remove all scanners first.")
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
    @Message(id = 21, value = "Deployment content %s appears to be incomplete and is not progressing toward completion. This" +
            " content cannot be auto-deployed.%s")
    String deploymentContentIncomplete(File file, String suffix);

    /**
     * A message indicating the deployment operation was not received with the timeout period.
     *
     * @param timeout the timeout period.
     *
     * @return the message.
     */
    @Message(id = 22, value = "Did not receive a response to the deployment operation within the allowed timeout period " +
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
    @Message(id = 23, value = "%s does not exist")
    IllegalArgumentException directoryDoesNotExist(String path);

    /**
     * Creates an exception indicating the directory, represented by the {@code path} parameter, is not writable.
     *
     * @param path the path of the directory.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 24, value = "%s is not writable")
    IllegalArgumentException directoryNotWritable(String path);

    /**
     * A message indicating the file, represented by the {@code fileName} parameter, cannot be scanned because it does
     * not begin with a ZIP file format local file header signature.
     *
     * @param fileName the name of the invalid file.
     *
     * @return the messages.
     */
    @Message(id = 25, value = "File %s cannot be scanned because it does not begin with a ZIP file format local file header signature")
    String invalidZipFileFormat(String fileName);

    /**
     * A message indicating the file, represented by the {@code fileName} parameter, cannot be scanned because it uses
     * the currently unsupported ZIP64 format.
     *
     * @param fileName the name of the invalid file.
     *
     * @return the messages.
     */
    @Message(id = 26, value = "File %s cannot be scanned because it uses the currently unsupported ZIP64 format")
    String invalidZip64FileFormat(String fileName);

    /**
     * Creates an exception indicating the {@code path} is not a directory.
     *
     * @param path the path name.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 27, value = "%s is not a directory")
    IllegalArgumentException notADirectory(String path);

    /**
     * Creates an exception indicating the variable, represented by the {@code name} parameter, is {@code null}.
     *
     * @param name the name of the {@code null} variable.
     *
     * @return an {@link IllegalArgumentException} for the error.
     */
    @Message(id = 28, value = "%s is null")
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
    @Message(id = 29, value = "scanner not configured")
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
    @Message(id = 30, value = "File %2$s was configured for auto-deploy but could not be safely auto-deployed. The reason the file " +
            "could not be auto-deployed was: %1$s.  To enable deployment of this file create a file called %2$s%3$s")
    String unsafeAutoDeploy2(String errorMsg, String fileName, String marker);

    @Message(id = 31, value = "Extension with module 'org.jboss.as.deployment-scanner' cannot be installed in a managed domain. Please remove it and any subsystem referencing it")
    IllegalStateException deploymentScannerNotForDomainMode();

    @Message(id = 32, value = "Failed to list files in directory %s. Check that the contents of the directory are readable.")
    RuntimeException cannotListDirectoryFiles(File directory);
}
