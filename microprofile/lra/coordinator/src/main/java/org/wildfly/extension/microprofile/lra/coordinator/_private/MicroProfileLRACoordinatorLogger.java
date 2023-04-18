/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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

/**
 * Log messages for WildFly microprofile-lra-coordinator Extension.
 */
@MessageLogger(projectCode = "WFLYTXLRACOORD", length = 4)
public interface MicroProfileLRACoordinatorLogger extends BasicLogger {

    MicroProfileLRACoordinatorLogger LOGGER = Logger.getMessageLogger(MicroProfileLRACoordinatorLogger.class, "org.wildfly.extension.microprofile.lra.coordinator");

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

}