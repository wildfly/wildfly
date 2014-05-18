/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2014, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.extension.io.logging;

import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author Tomaz Cerar (c) 2014 Red Hat Inc.
 */
@MessageLogger(projectCode = "WFLYIO", length = 3)
public interface IOLogger extends BasicLogger {

    /**
     * A root logger with the category of the package name.
     */
    IOLogger ROOT_LOGGER = Logger.getMessageLogger(IOLogger.class, "org.wildfly.extension.io");


    @LogMessage(level = INFO)
    @Message(id = 1, value = "Worker '%s' has auto-configured to %d core threads with %d task threads based on your %d available processors")
    void printDefaults(String workerName, int ioThreads, int workerThreads, int cpuCount);

    @LogMessage(level = INFO)
    @Message(id = 2, value = "Worker '%s' has auto-configured to %d core threads based on your %d available processors")
    void printDefaultsIoThreads(String workerName, int ioThreads, int cpuCount);

    @LogMessage(level = INFO)
    @Message(id = 3, value = "Worker '%s' has auto-configured to %d task threads based on your %d available processors")
    void printDefaultsWorkerThreads(String workerName, int workerThreads, int cpuCount);

    @LogMessage(level = WARN)
    @Message(id = 4, value = "Worker '%s' would auto-configure to %d task threads based on %d available processors, however your system does not have enough file descriptors configured to support this configuration. It is likely you will experience application degradation unless you increase your file descriptor limit.")
    void lowFD(String workerName, int suggestedWorkerThreadCount, int cpuCount);

    @LogMessage(level = WARN)
    @Message(id = 5, value = "Your system is configured with %d file descriptors, but your current application server configuration will require a minimum of %d (and probably more than that); attempting to adjust, however you should expect stability problems unless you increase this number")
    void lowGlobalFD(int maxFd, int requiredCount);

}
