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

/**
 * Log messages for WildFly microprofile-lra-participant Extension.
 */
@MessageLogger(projectCode = "WFLYTXLRAPARTICIPANT", length = 4)
public interface MicroProfileLRAParticipantLogger extends BasicLogger {

    MicroProfileLRAParticipantLogger LOGGER = Logger.getMessageLogger(MicroProfileLRAParticipantLogger.class, "org.wildfly.extension.microprofile.lra.participant");

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