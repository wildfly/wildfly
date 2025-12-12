/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.jakarta.data._private;

import static org.jboss.logging.Logger.Level.DEBUG;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "WFLYJDATA", length = 4)
public interface JakartaDataLogger extends BasicLogger {

    JakartaDataLogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), JakartaDataLogger.class, "org.wildfly.extension.jakarta.data");

    @LogMessage(level = DEBUG)
    @Message(id = 1, value = "The deployment does not have Jakarta Contexts and Dependency Injection enabled. Skipping Jakarta Data integration.")
    void noCdiDeployment();

    @LogMessage(level = DEBUG)
    @Message(id = 2, value = "Deployment %s requires use of the '%s' capability but it is not currently registered")
    void deploymentRequiresCapability(String deploymentName, String capabilityName);

    /**
     * Creates an exception indicating there was an error when trying to get the transaction associated with the
     * current thread.
     *
     * @param cause the cause of the error.
     * @return an {@link IllegalStateException} for the error.
     */
    @Message(id = 3, value = "An error occurred while getting the transaction associated with the current thread: %s")
    IllegalStateException errorGettingTransaction(Exception cause);
}
