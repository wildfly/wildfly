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

import java.io.File;
import java.util.Set;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface DeploymentScannerLogger extends BasicLogger {

    /**
     * A logger with the category of the package name.
     */
    DeploymentScannerLogger ROOT_LOGGER = Logger.getMessageLogger(DeploymentScannerLogger.class, DeploymentScannerLogger.class.getPackage().getName());

    /**
     * Logs a warning message indicating the extraneous deployment marker file could not be removed.
     *
     * @param file the file that could not be removed.
     */
    @LogMessage(level = WARN)
    @Message(id = 15000, value = "Cannot delete deployment progress marker file %s")
    void cannotDeleteDeploymentProgressMarker(File file);

    /**
     * Logs a warning message indicating the extraneous deployment marker file could not be removed.
     *
     * @param fileName the file that could not be removed.
     */
    @LogMessage(level = WARN)
    @Message(id = 15001, value = "Cannot remove extraneous deployment marker file %s")
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
    @Message(id = 15002, value = "Deployment of '%s' requested, but the deployment is not present")
    void deploymentNotFound(String deploymentName);

    /**
     * Logs a warning message indicating a deployment was triggered.
     *
     * @param fileName the file name.
     * @param marker   the marker.
     */
    @LogMessage(level = INFO)
    @Message(id = 15003, value = "Found %1$s in deployment directory. To trigger deployment create a file called %1$s%2$s")
    void deploymentTriggered(String fileName, String marker);

    /**
     * Logs an error message indicating an exception was caught writing a deployment marker file.
     *
     * @param cause      the cause of the error.
     * @param markerFile the marker file.
     */
    @LogMessage(level = ERROR)
    @Message(id = 15004, value = "Caught exception writing deployment marker file %s")
    void errorWritingDeploymentMarker(@Cause Throwable cause, String markerFile);

    /**
     * Logs a warning message indicating the behaviour is not possible when auto-deployment of exploded content is
     * enabled.
     *
     * @param marker      the marker.
     * @param elementName the element name.
     */
    @LogMessage(level = WARN)
    @Message(id = 15005, value = "Reliable deployment behaviour is not possible when auto-deployment of exploded content is enabled " +
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
    @Message(id = 15006, value = "The deployment scanner found that the content for exploded deployment %1$s has been " +
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
    @Message(id = 15007, value = "Failed checking whether %s was a complete zip")
    void failedCheckingZipFile(@Cause Throwable cause, String fileName);

    /**
     * Logs an error message indicating the file system deployment service failed.
     *
     * @param cause the cause of the error.
     */
    @LogMessage(level = ERROR)
    @Message(id = 15008, value = "File system deployment service failed")
    void fileSystemDeploymentFailed(@Cause Throwable cause);

    /**
     * Logs an informational message indicating scan found incompletely copied file content for deployment.
     *
     * @param name the name of the deployment.
     */
    @LogMessage(level = INFO)
    @Message(id = 15009, value = "Scan found incompletely copied file content for deployment %s. Deployment changes will not be " +
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
    @Message(id = 15010, value = "The deployment scanner found a directory named %1$s that was not inside a directory whose " +
            "name ends with .ear, .jar, .rar, .sar or .war. This is likely the result of unzipping an " +
            "archive directly inside the %2$s directory, which is a user error. " +
            "The %1$s directory will not be scanned for deployments, but it is possible that the scanner may" +
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
    @Message(id = 15011, value = "Scan of %s threw Exception")
    void scanException(@Cause Throwable cause, String fileName);

    /**
     * Logs an informational message indicating the {@code className} started for {@code deploymentPath}.
     *
     * @param className      the class name.
     * @param deploymentPath the deployment path.
     */
    @LogMessage(level = INFO)
    @Message(id = 15012, value = "Started %s for directory %s")
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
    @Message(id = 15013, value = "Scan found content configured for auto-deploy that could not be safely auto-deployed. See details above. " +
            "Deployment changes will not be processed until all problematic content is either removed or whether to " +
            "deploy the content or not is indicated via a %s or %s marker file. Problematic deployments are %s")
    void unsafeAutoDeploy(String marker1, String marker2, Set<String> problems);

    /**
     * Logs an info level message indicating that a failed deployment {@code deploymentName} is being re-attempted.
     * @param deploymentName The name of the failed deployment.
     */
    @LogMessage(level = INFO)
    @Message(id = 15014, value = "Re-attempting failed deployment %s")
    void reattemptingFailedDeployment(String deploymentName);

    /**
     * Logs an error message indicating a failure checking whether the xml file, represented by the {@code fileName}
     * parameter, was a complete xml file.
     *
     * @param cause    the cause of the error.
     * @param fileName the file name.
     */
    @LogMessage(level = ERROR)
    @Message(id = 15015, value = "Failed checking whether %s was a complete XML")
    void failedCheckingXMLFile(@Cause Throwable cause, String fileName);
}
