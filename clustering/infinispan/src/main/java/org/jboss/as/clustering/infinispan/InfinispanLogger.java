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

package org.jboss.as.clustering.infinispan;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

/**
 * Date: 29.08.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Tristan Tarrant
 */
@MessageLogger(projectCode = "JBAS")
public interface InfinispanLogger extends BasicLogger {
    String ROOT_LOGGER_CATEGORY = InfinispanLogger.class.getPackage().getName();

    /**
     * The root logger.
     */
    InfinispanLogger ROOT_LOGGER = Logger.getMessageLogger(InfinispanLogger.class, ROOT_LOGGER_CATEGORY);

    /**
     * Logs an informational message indicating the Infinispan subsystem is being activated.
     */
    @LogMessage(level = INFO)
    @Message(id = 10280, value = "Activating Infinispan subsystem.")
    void activatingSubsystem();

    /**
     * Logs an informational message indicating that a cache is being started.
     *
     * @param cacheName     the name of the cache.
     * @param containerName the name of the cache container.
     */
    @LogMessage(level = INFO)
    @Message(id = 10281, value = "Started %s cache from %s container")
    void cacheStarted(String cacheName, String containerName);


    /**
     * Logs an informational message indicating that a cache is being stopped.
     *
     * @param cacheName     the name of the cache.
     * @param containerName the name of the cache container.
     */
    @LogMessage(level = INFO)
    @Message(id = 10282, value = "Stopped %s cache from %s container")
    void cacheStopped(String cacheName, String containerName);


    /**
     * Logs a warning message indicating that the eager attribute of the transactional element
     * is no longer valid
     *
     */
    @LogMessage(level = WARN)
    @Message(id = 10283, value = "The 'eager' attribute specified on the 'transaction' element of a cache is no longer valid")
    void eagerAttributeDeprecated();
}
