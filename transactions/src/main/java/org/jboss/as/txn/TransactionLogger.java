/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.txn;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import static org.jboss.logging.Logger.Level.ERROR;

/**
 * Transaction logger. Uses id's 10100 to 10199.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface TransactionLogger extends BasicLogger {
    /**
     * A logger with the category of the default transaction package.
     */
    TransactionLogger ROOT_LOGGER = Logger.getMessageLogger(TransactionLogger.class, TransactionLogger.class.getPackage().getName());

    /**
     * If a transaction could not be rolled back
     *
     * @return the message.
     */
    @LogMessage(level = ERROR)
    @Message(id = 10150, value = "Unable to roll back active transaction")
    void unableToRollBack(@Cause Throwable cause);


    /**
     * If the current transaction status could not be determined
     *
     * @return the message.
     */
    @LogMessage(level = ERROR)
    @Message(id = 10151, value = "Unable to get transaction state")
    void unableToGetTransactionStatus(@Cause Throwable cause);


    /**
     * If the user left a transaction open
     *
     * @return the message.
     */
    @LogMessage(level = ERROR)
    @Message(id = 10152, value = "APPLICATION ERROR: transaction still active in request with status %s")
    void transactionStillOpen(int status);
}
