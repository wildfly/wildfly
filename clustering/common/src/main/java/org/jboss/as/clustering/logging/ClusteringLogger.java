/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.logging;

import static org.jboss.logging.Logger.Level.WARN;

import java.lang.invoke.MethodHandles;
import java.util.Set;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author Paul Ferraro
 */
@MessageLogger(projectCode = "WFLYCLCOM", length = 4)
public interface ClusteringLogger extends BasicLogger {

    String ROOT_LOGGER_CATEGORY = "org.jboss.as.clustering";

    /**
     * The root logger.
     */
    ClusteringLogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), ClusteringLogger.class, ROOT_LOGGER_CATEGORY);

    @Message(id = 1, value = "%2$g is not a valid value for parameter %1$s. The value must be %3$s %4$g")
    OperationFailedException parameterValueOutOfBounds(String name, double value, String relationalOperator, double bound);

    @Message(id = 2, value = "Failed to close %s")
    @LogMessage(level = WARN)
    void failedToClose(@Cause Throwable cause, Object value);

    @Message(id = 3, value = "The following attributes do not support negative values: %s")
    String attributesDoNotSupportNegativeValues(Set<String> attributes);

    @Message(id = 4, value = "The following attributes do not support zero values: %s")
    String attributesDoNotSupportZeroValues(Set<String> attributes);

    @Message(id = 5, value = "Legacy host does not support multiple values for attributes: %s")
    String rejectedMultipleValues(Set<String> attributes);

    @LogMessage(level = WARN)
    @Message(id = 6, value = "The '%s' attribute of the '%s' element is no longer supported and will be ignored")
    void attributeIgnored(String attribute, String element);

    @LogMessage(level = WARN)
    @Message(id = 7, value = "The '%s' element is no longer supported and will be ignored")
    void elementIgnored(String element);

    @Message(id = 8, value = "%s:%s operation is only supported in admin-only mode.")
    OperationFailedException operationNotSupportedInNormalServerMode(String address, String operation);
}
