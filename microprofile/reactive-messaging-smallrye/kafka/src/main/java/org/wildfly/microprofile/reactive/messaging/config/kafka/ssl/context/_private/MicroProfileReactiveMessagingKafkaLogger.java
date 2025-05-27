/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.reactive.messaging.config.kafka.ssl.context._private;

import static org.jboss.logging.Logger.Level.INFO;

import java.lang.invoke.MethodHandles;

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
@MessageLogger(projectCode = "WFLYRXMKAF", length = 4)
public interface MicroProfileReactiveMessagingKafkaLogger extends BasicLogger {

    MicroProfileReactiveMessagingKafkaLogger LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), MicroProfileReactiveMessagingKafkaLogger.class, "org.wildfly.extension.microprofile.reactive.messaging");

    /**
     * Logs an informational message indicating the subsystem is being activated.
     */
    @LogMessage(level = INFO)
    @Message(id = 1, value = "Found property %s, will use the Elytron client-ssl-context: %s")
    void foundPropertyUsingElytronClientSSLContext(String prop, String ctx);

    @Message(id = 2, value = "Could not find an Elytron client-ssl-context called: %s")
    IllegalStateException noElytronClientSSLContext(String ctx);

    @Message(id = 3, value = "'%s' compression is not supported when running on Windows or Mac OS. The MicroProfile Config " +
            "property configuring this compression type is: %s")
    RuntimeException compressionNotSupportedOnWindows(String propertyValue, String propertyName);
}
