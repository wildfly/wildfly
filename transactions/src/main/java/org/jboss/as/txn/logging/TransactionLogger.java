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

package org.jboss.as.txn.logging;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.WARN;

import java.io.IOException;

import javax.transaction.Synchronization;
import javax.transaction.Transaction;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.msc.service.StartException;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "WFLYTX", length = 4)
public interface TransactionLogger extends BasicLogger {
    /**
     * A logger with the category of the default transaction package.
     */
    TransactionLogger ROOT_LOGGER = Logger.getMessageLogger(TransactionLogger.class, "org.jboss.as.txn");

    /**
     * If a transaction could not be rolled back
     */
    @LogMessage(level = ERROR)
    @Message(id = 1, value = "Unable to roll back active transaction")
    void unableToRollBack(@Cause Throwable cause);


    /**
     * If the current transaction status could not be determined
     */
    @LogMessage(level = ERROR)
    @Message(id = 2, value = "Unable to get transaction state")
    void unableToGetTransactionStatus(@Cause Throwable cause);


    /**
     * If the user left a transaction open
     */
    @LogMessage(level = ERROR)
    @Message(id = 3, value = "APPLICATION ERROR: transaction still active in request with status %s")
    void transactionStillOpen(int status);

    /**
     * Creates an exception indicating a create failed.
     *
     * @param cause the reason the creation failed.
     *
     * @return a {@link org.jboss.msc.service.StartException} initialized with the cause.
     */
    @Message(id = 4, value = "Create failed")
    StartException createFailed(@Cause Throwable cause);

    /**
     * Creates an exception indicating the start of a manager failed.
     *
     * @param cause       the reason the start failed.
     * @param managerName the name of the manager that didn't start.
     *
     * @return a {@link org.jboss.msc.service.StartException} initialized with the cause and error message.
     */
    @Message(id = 5, value = "%s manager create failed")
    StartException managerStartFailure(@Cause Throwable cause, String managerName);

    /**
     * Creates an exception indicating the failure of the object store browser.
     *
     * @param cause the reason the start failed.
     *
     * @return a {@link org.jboss.msc.service.StartException} initialized with the cause and error message.
     */
    @Message(id = 6, value = "Failed to configure object store browser bean")
    StartException objectStoreStartFailure(@Cause Throwable cause);


    /**
     * Creates an exception indicating that a service was not started.
     *
     * @return a {@link IllegalStateException} initialized with the cause and error message.
     */
    @Message(id = 7, value = "Service not started")
    IllegalStateException serviceNotStarted();

    /**
     * Creates an exception indicating the start failed.
     *
     * @param cause the reason the start failed.
     *
     * @return a {@link org.jboss.msc.service.StartException} initialized with the cause.
     */
    @Message(id = 8, value = "Start failed")
    StartException startFailure(@Cause Throwable cause);

    /**
     * A message indicating the metric is unknown.
     *
     * @param metric the unknown metric.
     *
     * @return the message.
     */
    @Message(id = 9, value = "Unknown metric %s")
    String unknownMetric(Object metric);

    @Message(id = 10, value = "MBean Server service not installed, this functionality is not available if the JMX subsystem has not been installed.")
    RuntimeException jmxSubsystemNotInstalled();

    @Message(id = 11, value = "'hornetq-store-enable-async-io' must be true.")
    String transformHornetQStoreEnableAsyncIoMustBeTrue();

    @Message(id = 12, value = "Attributes %s and %s are alternatives; both cannot be set with conflicting values.")
    OperationFailedException inconsistentStatisticsSettings(String attrOne, String attrTwo);

    /**
     * If the user has set node identifier to the default value
     *
     * @return the message.
     */
    @LogMessage(level = WARN)
    @Message(id = 13, value = "Node identifier property is set to the default value. Please make sure it is unique.")
    void nodeIdentifierIsSetToDefault();

    /**
     * A message indicating that jndi-name is missing and it's a required attribute
     *
     * @return the message.
     */
    @Message(id = 14, value = "Jndi name is required")
    OperationFailedException jndiNameRequired();

    /**
     * A message indicating that jndi-name has an invalid format
     *
     * @return the message.
     */
    @Message(id = 15, value = "Jndi names have to start with java:/ or java:jboss/")
    OperationFailedException jndiNameInvalidFormat();

    @LogMessage(level = WARN)
    @Message(id = 16, value = "Transaction started in EE Concurrent invocation left open, starting rollback to prevent leak.")
    void rollbackOfTransactionStartedInEEConcurrentInvocation();

    @LogMessage(level = WARN)
    @Message(id = 17, value = "Failed to rollback transaction.")
    void failedToRollbackTransaction(@Cause Throwable cause);

    @LogMessage(level = WARN)
    @Message(id = 18, value = "Failed to suspend transaction.")
    void failedToSuspendTransaction(@Cause Throwable cause);

    @LogMessage(level = WARN)
    @Message(id = 19, value = "System error while checking for transaction leak in EE Concurrent invocation.")
    void systemErrorWhileCheckingForTransactionLeak(@Cause Throwable cause);

    @Message(id = 20, value = "EE Concurrent ContextHandle serialization must be handled by the factory.")
    IOException serializationMustBeHandledByTheFactory();

    @Message(id = 21, value = "EE Concurrent's TransactionSetupProviderService not started.")
    IllegalStateException transactionSetupProviderServiceNotStarted();

    @Message(id = 22, value = "EE Concurrent's TransactionSetupProviderService not installed.")
    IllegalStateException transactionSetupProviderServiceNotInstalled();

    @Message(id = 23, value = "%s must be undefined if %s is 'true'.")
    OperationFailedException mustBeUndefinedIfTrue(String attrOne, String attrTwo);

    @Message(id = 24, value = "%s must be defined if %s is defined.")
    OperationFailedException mustBedefinedIfDefined(String attrOne, String attrTwo);

    @Message(id = 25, value = "Either %s must be 'true' or  %s must be defined.")
    OperationFailedException eitherTrueOrDefined(String attrOne, String attrTwo);

    @LogMessage(level = WARN)
    @Message(id = 26, value = "The transaction %s could not be removed from the cache during cleanup.")
    void transactionNotFound(Transaction tx);

    @LogMessage(level = WARN)
    @Message(id = 27, value = "The pre-jca synchronization %s associated with tx %s failed during after completion")
    void preJcaSyncAfterCompletionFailed(Synchronization preJcaSync, Transaction tx, @Cause Exception e);

    @LogMessage(level = WARN)
    @Message(id = 28, value = "The jca synchronization %s associated with tx %s failed during after completion")
    void jcaSyncAfterCompletionFailed(Synchronization jcaSync, Transaction tx, @Cause Exception e);
}
