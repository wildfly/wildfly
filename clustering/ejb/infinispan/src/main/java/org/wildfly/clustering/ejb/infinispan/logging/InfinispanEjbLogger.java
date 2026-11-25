/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.ejb.infinispan.logging;

import static org.jboss.logging.Logger.Level.WARN;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Logger for this module.
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "WFLYCLEJBINF", length = 4)
public interface InfinispanEjbLogger extends BasicLogger {

    String ROOT_LOGGER_CATEGORY = "org.wildfly.clustering.ejb.infinispan";

    /**
     * A logger with the category of the default clustering package.
     */
    InfinispanEjbLogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), InfinispanEjbLogger.class, ROOT_LOGGER_CATEGORY);

//    @LogMessage(level = WARN)
//    @Message(id = 1, value = "Failed to passivate stateful session bean %s")
//    void failedToPassivateBean(@Cause Throwable cause, Object id);

//    @LogMessage(level = WARN)
//    @Message(id = 2, value = "Failed to passivate stateful session bean group %s")
//    void failedToPassivateBeanGroup(@Cause Throwable cause, Object id);

    @LogMessage(level = WARN)
    @Message(id = 3, value = "Failed to expire stateful session bean %s")
    void failedToExpireBean(@Cause Throwable cause, Object id);

//    @Message(id = 4, value = "Failed to deserialize %s")
//    IllegalStateException deserializationFailure(@Cause Throwable cause, Object key);

//    @LogMessage(level = DEBUG)
//    @Message(id = 5, value = "Failed to cancel expiration/passivation of bean %s on primary owner.")
//    void failedToCancelBean(@Cause Throwable cause, Object beanId);

//    @LogMessage(level = DEBUG)
//    @Message(id = 6, value = "Failed to schedule expiration/passivation of bean %s on primary owner.")
//    void failedToScheduleBean(@Cause Throwable cause, Object beanId);

//    @LogMessage(level = WARN)
//    @Message(id = 8, value = "Stateful session bean %s refers to an invalid bean group %s")
//    void invalidBeanGroup(Object beanId, Object groupId);

//    @LogMessage(level = WARN)
//    @Message(id = 9, value = "Disabling eviction for cache '%s'. SFSB passivation should be configured via the associated Jakarta Enterprise Beans subsystem passivation-store.")
//    void evictionDisabled(String cacheName);

    @LogMessage(level = WARN)
    @Message(id = 10, value = "Disabling expiration for '%s'. SFSB expiration should be configured per \u00A74.3.11 of the Jakarta Enterprise Beans specification.")
    void expirationDisabled(String cacheName);
}
