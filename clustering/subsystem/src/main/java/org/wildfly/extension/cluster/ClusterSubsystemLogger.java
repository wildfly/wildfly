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

package org.wildfly.extension.cluster;

import static org.jboss.logging.Logger.Level.INFO;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * ClusterSubsystemLogger
 *
 * logging id range: 20700 - 20709
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MessageLogger(projectCode = "JBAS")
public interface ClusterSubsystemLogger extends BasicLogger {

    String ROOT_LOGGER_CATEGORY = ClusterSubsystemLogger.class.getPackage().getName();

    /**
     * A logger with the category of the default clustering package.
     */
    ClusterSubsystemLogger ROOT_LOGGER = Logger.getMessageLogger(ClusterSubsystemLogger.class, ROOT_LOGGER_CATEGORY);

    /**
     * Logs an info message indicating startup of the cluster subsystem..
     */
    @LogMessage(level = INFO)
    @Message(id = 20700, value = "Activating cluster subsystem.")
    void activatingSubsystem();

}
