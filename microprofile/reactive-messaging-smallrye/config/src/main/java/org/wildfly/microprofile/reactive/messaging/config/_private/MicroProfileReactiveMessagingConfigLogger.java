/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.reactive.messaging.config._private;

import static org.jboss.logging.Logger.Level.WARN;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.wildfly.microprofile.reactive.messaging.config.TracingType;

/**
 * Log messages for WildFly microprofile-reactive-messaging-smallrye Extension.
 *
 * @author <a href="kkhan@redhat.com">Kabir Khan</a>
 */
@MessageLogger(projectCode = "WFLYRXMCFG", length = 4)
public interface MicroProfileReactiveMessagingConfigLogger extends BasicLogger {

    MicroProfileReactiveMessagingConfigLogger LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), MicroProfileReactiveMessagingConfigLogger.class, "org.wildfly.extension.microprofile.reactive.messaging.config");

    @LogMessage(level = WARN)
    @Message(id = 1, value = "Property %s is set to %s. Since the value for %s in the microprofile-reactive-messaging subsystem is set to %s, the property is overridden to return %s")
    void tracingTypeOverridesProperty(String propertyName, String value, String connectorAttribute, TracingType type, String fromTracingType);
}
