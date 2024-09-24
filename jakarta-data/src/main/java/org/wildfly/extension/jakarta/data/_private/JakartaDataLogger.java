/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.jakarta.data._private;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

@MessageLogger(projectCode = "WFLYJDATA", length = 4)
public interface JakartaDataLogger extends BasicLogger {

    JakartaDataLogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), JakartaDataLogger.class, "org.wildfly.extension.jakarta.data");
}
