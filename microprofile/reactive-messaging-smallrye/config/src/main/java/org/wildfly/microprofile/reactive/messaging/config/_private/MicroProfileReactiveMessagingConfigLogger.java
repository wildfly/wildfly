/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.reactive.messaging.config._private;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author <a href="kkhan@redhat.com">Kabir Khan</a>
 */
@MessageLogger(projectCode = "WFLYRXMCFG", length = 4)
public interface MicroProfileReactiveMessagingConfigLogger extends BasicLogger {

    MicroProfileReactiveMessagingConfigLogger LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), MicroProfileReactiveMessagingConfigLogger.class, "org.wildfly.extension.microprofile.reactive.messaging.config");

    // Empty, but here so we have a logger to check for if debugging is enabled
}
