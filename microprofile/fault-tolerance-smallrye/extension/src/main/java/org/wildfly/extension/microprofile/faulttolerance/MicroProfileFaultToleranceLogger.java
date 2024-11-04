/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.faulttolerance;

import static org.jboss.logging.Logger.Level.INFO;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author Radoslav Husar
 */
@MessageLogger(projectCode = "WFLYMPFTEXT", length = 4)
public interface MicroProfileFaultToleranceLogger extends BasicLogger {

    MicroProfileFaultToleranceLogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), MicroProfileFaultToleranceLogger.class, MicroProfileFaultToleranceLogger.class.getPackage().getName());

    @LogMessage(level = INFO)
    @Message(id = 1, value = "Activating MicroProfile Fault Tolerance subsystem.")
    void activatingSubsystem();

    @LogMessage(level = INFO)
    @Message(id = 2, value = "MicroProfile Fault Tolerance subsystem will use %s metrics provider.")
    void metricsProvider(Set<String> metricsProvider);

}
