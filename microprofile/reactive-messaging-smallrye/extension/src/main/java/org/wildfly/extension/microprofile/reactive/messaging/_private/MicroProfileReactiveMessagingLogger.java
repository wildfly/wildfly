/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.reactive.messaging._private;

import static org.jboss.logging.Logger.Level.INFO;

import java.lang.invoke.MethodHandles;

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.jandex.DotName;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Log messages for WildFly microprofile-reactive-messaging-smallrye Extension.
 *
 * @author <a href="kkhan@redhat.com">Kabir Khan</a>
 */
@MessageLogger(projectCode = "WFLYRXMESS", length = 4)
public interface MicroProfileReactiveMessagingLogger extends BasicLogger {

    MicroProfileReactiveMessagingLogger LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), MicroProfileReactiveMessagingLogger.class, "org.wildfly.extension.microprofile.reactive.messaging");

    /**
     * Logs an informational message indicating the subsystem is being activated.
     */
    @LogMessage(level = INFO)
    @Message(id = 1, value = "Activating MicroProfile Reactive Messaging Subsystem")
    void activatingSubsystem();


    @Message(id = 2, value = "Deployment %s requires use of the '%s' capability but it is not currently registered")
    DeploymentUnitProcessingException deploymentRequiresCapability(String deploymentName, String capabilityName);

    @LogMessage(level = INFO)
    @Message(id = 3, value = "Intermediate module %s is not present. Skipping recursively adding modules from it")
    void intermediateModuleNotPresent(String intermediateModuleName);

    @Message(id = 4, value = "Use of -D%s=true is not allowed in this setup")
    DeploymentUnitProcessingException experimentalPropertyNotAllowed(String experimentalProperty);

    @Message(id = 5, value = "Use of @%s is not allowed in this setup")
    DeploymentUnitProcessingException experimentalAnnotationNotAllowed(DotName dotName);
}
