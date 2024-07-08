/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */


package org.wildfly.microprofile.reactive.messaging.config.amqp.ssl.context._private;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Log messages for WildFly microprofile-reactive-messaging-smallrye Extension.
 *
 * @author <a href="kkhan@redhat.com">Kabir Khan</a>
 */
@MessageLogger(projectCode = "WFLYRMAMQP", length = 4)
public interface MicroProfileReactiveMessagingAmqpLogger extends BasicLogger {

    MicroProfileReactiveMessagingAmqpLogger LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), MicroProfileReactiveMessagingAmqpLogger.class, "org.wildfly.extension.microprofile.reactive.messaging");

    // No log messages here yet

}
