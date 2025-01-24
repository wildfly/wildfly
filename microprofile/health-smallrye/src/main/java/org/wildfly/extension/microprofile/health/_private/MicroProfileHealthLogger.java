/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.health._private;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.lang.invoke.MethodHandles;

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Log messages for WildFly microprofile-health-smallrye Extension.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2018 Red Hat inc.
 */
@MessageLogger(projectCode = "WFLYMPHEALTH", length = 4)
public interface MicroProfileHealthLogger extends BasicLogger {
    /**
     * A logger with the category {@code org.wildfly.extension.batch}.
     */
    MicroProfileHealthLogger LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), MicroProfileHealthLogger.class, "org.wildfly.extension.microprofile.health.smallrye");

    /**
     * Logs an informational message indicating the naming subsystem is being activated.
     */
    @LogMessage(level = INFO)
    @Message(id = 1, value = "Activating MicroProfile Health Subsystem")
    void activatingSubsystem();

    @Message(id = 2, value = "Deployment %s requires use of the '%s' capability but it is not currently registered")
    DeploymentUnitProcessingException deploymentRequiresCapability(String deploymentName, String capabilityName);

    @LogMessage(level = WARN)
    @Message(id = 3, value = "Reporting health down status: %s")
    void healthDownStatus(String cause);

    // 4, 5 and 6 are taken downstream
    /*
    @Message(id = 4, value = "")
    OperationFailedException seeDownstream();

    @Message(id = 5, value = "")
    String seeDownstream();

    @Message(id = 6, value = "")
    OperationFailedException seeDownstream();
    */

    @LogMessage(level = INFO)
    @Message(id = 7, value = "The deployment %s configuration has specified that default MicroProfile Health procedures should be disabled; server-wide procedures will be disabled.")
    void addDefaultProceduresDisabledByDeployment(String deploymentName);

    @LogMessage(level = INFO)
    @Message(id = 8, value = "The deployment %s configuration which specified that default MicroProfile Health procedures had to be disabled has been undeployed.")
    void removeDefaultProceduresDisabledByDeployment(String deploymentName);
}
