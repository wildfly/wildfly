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

package org.jboss.as.jdkorb;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

import static org.jboss.logging.Logger.Level.DEBUG;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.TRACE;

/**
 * The jacorb subsystem is using message IDs in the range 16300-16499. This file is using the subset 16300-16399 for
 * logger messages.
 * See http://http://community.jboss.org/wiki/LoggingIds for the full list of currently reserved JBAS message id blocks.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface JDKORBLogger extends BasicLogger {

    JDKORBLogger ROOT_LOGGER = Logger.getMessageLogger(JDKORBLogger.class, JDKORBLogger.class.getPackage().getName());

    @LogMessage(level = INFO)
    @Message(id = 16300, value = "Activating JacORB Subsystem")
    void activatingSubsystem();

    @LogMessage(level = TRACE)
    @Message(id = 16322, value = "Creating server socket factory: %s")
    void traceServerSocketFactoryCreation(String factoryClass);

    @LogMessage(level = DEBUG)
    @Message(id = 16326, value = "Starting service %s")
    void debugServiceStartup(String serviceName);

    @LogMessage(level = DEBUG)
    @Message(id = 16327, value = "Stopping service %s")
    void debugServiceStop(String serviceName);

    @LogMessage(level = INFO)
    @Message(id = 16330, value = "CORBA ORB Service started")
    void corbaORBServiceStarted();
}
