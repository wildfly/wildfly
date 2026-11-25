/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.injection._private;

import java.lang.invoke.MethodHandles;

import jakarta.jms.IllegalStateRuntimeException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Date: 10.06.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "WFLYMSGAMQINJ", length = 4)
public interface MessagingLogger extends BasicLogger {
    /**
     * The logger with the category of the package.
     */
    MessagingLogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), MessagingLogger.class, "org.wildfly.extension.messaging-activemq");

    /**
     * Create an exception when calling a method not allowed on injected JMSContext.
     *
     * @return an {@link IllegalStateRuntimeException} for the error.
     */
    @Message(id = 1, value = "It is not permitted to call this method on injected JMSContext (see Jakarta Messaging 2.0 spec, §12.4.5).")
    IllegalStateRuntimeException callNotPermittedOnInjectedJMSContext();
}
