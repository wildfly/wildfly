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
package org.jboss.as.clustering.singleton;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.msc.service.StartException;

/**
 * SingletonLogger
 *
 * logging id ranges: 10340 - 10349
 *
 * @author <a href="mailto:pferraro@redhat.com">Paul Ferraro</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface SingletonLogger {
    String ROOT_LOGGER_CATEGORY = SingletonLogger.class.getPackage().getName();

    /**
     * The root logger.
     */
    SingletonLogger ROOT_LOGGER = Logger.getMessageLogger(SingletonLogger.class, ROOT_LOGGER_CATEGORY);

    @LogMessage(level = INFO)
    @Message(id = 10340, value = "This node will now operate as the singleton provider of the %s service")
    void electedMaster(String service);

    @LogMessage(level = INFO)
    @Message(id = 10341, value = "This node will no longer operate as the singleton provider of the %s service")
    void electedSlave(String service);

    @LogMessage(level = INFO)
    @Message(id = 10342, value = "%s elected as the singleton provider of the %s service")
    void elected(String node, String service);

    @LogMessage(level = DEBUG)
    @Message(id = 10343, value = "No response received from master node of the %s service, retrying...")
    void noResponseFromMaster(String service);

    @LogMessage(level = ERROR)
    @Message(id = 10344, value = "Failed to start %s service")
    void serviceStartFailed(@Cause StartException e, String service);

    @LogMessage(level = ERROR)
    @Message(id = 10345, value = "Failed to reach quorum of %2$d for %1$s service. No singleton master will be elected.")
    void quorumNotReached(String service, int quorum);

    @LogMessage(level = ERROR)
    @Message(id = 10346, value = "Just reached required quorum of %2$d for %1$s service. If this cluster loses another member, no node will be chosen to provide this service.")
    void quorumJustReached(String service, int quorum);
}
