/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.coordinator._private;

import io.narayana.lra.LRAConstants;
import jakarta.servlet.ServletException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.msc.service.StartException;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.lang.invoke.MethodHandles;

/**
 * Log messages for WildFly microprofile-lra-coordinator Extension.
 */
@MessageLogger(projectCode = "WFLYTXLRACOORD", length = 4)
public interface MicroProfileLRACoordinatorLogger extends BasicLogger {

    MicroProfileLRACoordinatorLogger LOGGER = Logger.getMessageLogger(MethodHandles.lookup(),MicroProfileLRACoordinatorLogger.class, "org.wildfly.extension.microprofile.lra.coordinator");

    /**
     * Logs an informational message indicating the subsystem is being activated.
     */
    @LogMessage(level = INFO)
    @Message(id = 1, value = "Activating MicroProfile LRA Coordinator Subsystem")
    void activatingSubsystem();

    /**
     * Creates an exception indicating that the LRA recovery service failed to start.
     *
     * @return a {@link org.jboss.msc.service.StartException} for the error.
     */
    @Message(id = 2, value = "LRA recovery service start failed")
    StartException lraRecoveryServiceFailedToStart();

    @LogMessage(level = INFO)
    @Message(id = 3, value = "Starting Narayana MicroProfile LRA Coordinator available at path %s/" + LRAConstants.COORDINATOR_PATH_NAME)
    void startingCoordinator(String path);

    @LogMessage(level = ERROR)
    @Message(id = 4, value = "Failed to stop Narayana MicroProfile LRA Coordinator at path %s/" + LRAConstants.COORDINATOR_PATH_NAME)
    void failedStoppingCoordinator(String path, @Cause ServletException cause);

    @LogMessage(level = WARN)
    @Message(id = 5, value = "Failed to start a recovery scan on the Narayana MicroProfile LRA Coordinator at path %s/"
            + LRAConstants.COORDINATOR_PATH_NAME)
    void failedToRunRecoveryScan(String path, @Cause Exception cause);
}
