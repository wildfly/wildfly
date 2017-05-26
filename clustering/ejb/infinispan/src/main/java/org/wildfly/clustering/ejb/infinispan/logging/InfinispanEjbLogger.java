/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.ejb.infinispan.logging;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.WARN;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "WFLYCLEJBINF", length = 4)
public interface InfinispanEjbLogger extends BasicLogger {

    String ROOT_LOGGER_CATEGORY = "org.wildfly.clustering.ejb.infinispan";

    /**
     * A logger with the category of the default clustering package.
     */
    InfinispanEjbLogger ROOT_LOGGER = Logger.getMessageLogger(InfinispanEjbLogger.class, ROOT_LOGGER_CATEGORY);

    @LogMessage(level = WARN)
    @Message(id = 1, value = "Failed to passivate stateful session bean %s")
    void failedToPassivateBean(@Cause Throwable cause, Object id);

    @LogMessage(level = WARN)
    @Message(id = 2, value = "Failed to passivate stateful session bean group %s")
    void failedToPassivateBeanGroup(@Cause Throwable cause, Object id);

    @LogMessage(level = WARN)
    @Message(id = 3, value = "Failed to expire stateful session bean %s")
    void failedToExpireBean(@Cause Throwable cause, Object id);

    @Message(id = 4, value = "Failed to deserialize %s")
    IllegalStateException deserializationFailure(@Cause Throwable cause, Object key);

    @LogMessage(level = DEBUG)
    @Message(id = 5, value = "Failed to cancel expiration/passivation of bean %s on primary owner.")
    void failedToCancelBean(@Cause Throwable cause, Object beanId);

    @LogMessage(level = DEBUG)
    @Message(id = 6, value = "Failed to schedule expiration/passivation of bean %s on primary owner.")
    void failedToScheduleBean(@Cause Throwable cause, Object beanId);

    @LogMessage(level = WARN)
    @Message(id = 8, value = "Stateful session bean %s refers to an invalid bean group %s")
    void invalidBeanGroup(Object beanId, Object groupId);

    @LogMessage(level = WARN)
    @Message(id = 9, value = "Disabling eviction for cache '%s'. SFSB passivation should be configured via the associated EJB subsystem passivation-store.")
    void evictionDisabled(String cacheName);

    @LogMessage(level = WARN)
    @Message(id = 10, value = "Disabling expiration for '%s'. SFSB expiration should be configured per \u00A74.3.11 of the EJB specification.")
    void expirationDisabled(String cacheName);
}
