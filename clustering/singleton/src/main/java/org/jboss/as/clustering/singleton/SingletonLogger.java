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

import static org.jboss.logging.Logger.Level.INFO;

import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

@MessageLogger(projectCode = "JBAS")
public interface SingletonLogger {
    String ROOT_LOGGER_CATEGORY = SingletonLogger.class.getPackage().getName();

    /**
     * The root logger.
     */
    SingletonLogger ROOT_LOGGER = Logger.getMessageLogger(SingletonLogger.class, ROOT_LOGGER_CATEGORY);

    @LogMessage(level = INFO)
    @Message(id = 10390, value = "This node will now operate as the singleton provider of the %s service")
    void electedMaster(String service);

    @LogMessage(level = INFO)
    @Message(id = 10391, value = "This node will no longer operate as the singleton provider of the %s service")
    void electedSlave(String service);
}
