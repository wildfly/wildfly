/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.logging;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author Paul Ferraro
 */
@MessageLogger(projectCode = "WFLYCLWEB", length = 4)
public interface Logger extends BasicLogger {
    String ROOT_LOGGER_CATEGORY = "org.wildfly.clustering.web.cache";

    Logger ROOT_LOGGER = org.jboss.logging.Logger.getMessageLogger(MethodHandles.lookup(), Logger.class, ROOT_LOGGER_CATEGORY);

    @Message(id = 1, value = "Session %s is not valid")
    IllegalStateException invalidSession(String sessionId);
}
