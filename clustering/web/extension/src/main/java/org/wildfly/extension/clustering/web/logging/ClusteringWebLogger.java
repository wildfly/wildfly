/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web.logging;

import static org.jboss.logging.Logger.Level.WARN;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Logger for this module.
 */
@MessageLogger(projectCode = "WFLYCLWEB", length = 4)
public interface ClusteringWebLogger extends BasicLogger {

    String ROOT_LOGGER_CATEGORY = "org.wildfly.extension.clustering.web";

    ClusteringWebLogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), ClusteringWebLogger.class, ROOT_LOGGER_CATEGORY);

    @LogMessage(level = WARN)
    @Message(id = 1, value = "Disabling expiration configuration otherwise specified in '%s' cache '%s'. Web session expiration must be managed by the servlet container per \u00A77.5 of the Jakarta Servlet specification.")
    void expirationDisabled(String containerName, String cacheName);
}
