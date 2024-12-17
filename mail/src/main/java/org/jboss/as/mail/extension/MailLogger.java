/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.mail.extension;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.msc.service.StartException;

/**
 * Date: 05.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "WFLYMAIL", length = 4)
interface MailLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    MailLogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), MailLogger.class, "org.jboss.as.mail.extension");

    /**
     * Logs an info message indicating a jakarta.mail.Session was bound into JNDI.
     *
     * @param jndiName the JNDI name under which the session was bound.
     */
    @LogMessage(level = INFO)
    @Message(id = 1, value = "Bound mail session [%s]")
    void boundMailSession(String jndiName);

    /**
     * Logs an info message indicating a jakarta.mail.Session was unbound from JNDI.
     *
     * @param jndiName the JNDI name under which the session was bound.
     */
    @LogMessage(level = INFO)
    @Message(id = 2, value = "Unbound mail session [%s]")
    void unboundMailSession(String jndiName);

    /**
     * Logs a debug message indicating a jakarta.mail.Session was removed.
     *
     * @param jndiName the JNDI name under which the session had been bound.
     */
    @LogMessage(level = DEBUG)
    @Message(id = 3, value = "Removed mail session [%s]")
    void removedMailSession(String jndiName);

    /**
     * Creates an exception indicating the outgoing socket binding, represented by the {@code outgoingSocketBindingRef}
     * parameter, could not be found.
     *
     * @param outgoingSocketBindingRef the name of the socket binding configuration.
     * @return a {@link StartException} for the error.
     */
    @Message(id = 4, value = "No outbound socket binding configuration '%s' is available.")
    StartException outboundSocketBindingNotAvailable(String outgoingSocketBindingRef);

    /**
     * Logs an error message indicating that the configured host name could not be resolved.
     *
     * @param hostName the name of the host which coud not be resolved.
     */
    @LogMessage(level = WARN)
    @Message(id = 9, value = "Host name [%s] could not be resolved!")
    void hostUnknown(String hostName);
}
