/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.rts.logging;

import jakarta.ws.rs.container.ContainerResponseContext;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import static org.jboss.logging.Logger.Level.ERROR;

import java.lang.invoke.MethodHandles;

/**
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 *
 */
@MessageLogger(projectCode = "WFLYRTS", length = 4)
public interface RTSLogger extends BasicLogger {

    RTSLogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), RTSLogger.class, "org.wildfly.extension.rts");

    @Message(id = 1, value = "Can't import global transaction to wildfly transaction client.")
    IllegalStateException failueOnImportingGlobalTransactionFromWildflyClient(@Cause jakarta.transaction.SystemException se);

    @LogMessage(level = ERROR)
    @Message(id = 2, value = "Cannot get transaction status on handling response context %s")
    void cannotGetTransactionStatus(ContainerResponseContext responseCtx, @Cause Throwable cause);
}
