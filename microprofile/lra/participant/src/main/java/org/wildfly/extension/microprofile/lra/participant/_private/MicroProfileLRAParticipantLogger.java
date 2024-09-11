/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.participant._private;

import jakarta.servlet.ServletException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.wildfly.extension.microprofile.lra.participant.service.LRAParticipantService;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.lang.invoke.MethodHandles;

/**
 * Log messages for WildFly microprofile-lra-participant Extension.
 */
@MessageLogger(projectCode = "WFLYTXLRAPARTICIPANT", length = 4)
public interface MicroProfileLRAParticipantLogger extends BasicLogger {

    MicroProfileLRAParticipantLogger LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), MicroProfileLRAParticipantLogger.class, "org.wildfly.extension.microprofile.lra.participant");

    /**
     * Logs an informational message indicating the subsystem is being activated.
     */
    @LogMessage(level = INFO)
    @Message(id = 1, value = "Activating MicroProfile LRA Participant Subsystem with system property (lra.coordinator.url) value as %s")
    void activatingSubsystem(String url);

    @LogMessage(level = INFO)
    @Message(id = 2, value = "Starting Narayana MicroProfile LRA Participant Proxy available at path %s" + LRAParticipantService.CONTEXT_PATH)
    void startingParticipantProxy(String path);

    @LogMessage(level = WARN)
    @Message(id = 3, value = "The CDI marker file cannot be created")
    void cannotCreateCDIMarkerFile(@Cause Throwable e);

    @LogMessage(level = ERROR)
    @Message(id = 4, value = "Failed to stop Narayana MicroProfile LRA Participant Proxy at path %s/" + LRAParticipantService.CONTEXT_PATH)
    void failedStoppingParticipant(String path, @Cause ServletException cause);

}
