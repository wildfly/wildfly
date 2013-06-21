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

package org.wildfly.as.concurrent;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

import static org.jboss.logging.Logger.Level.DEBUG;

/**
 * This module is using message IDs in the range ?-?.
 * <p/>
 * This file is using the subset ?-? for logger messages.
 * <p/>
 * See <a href="http://community.jboss.org/docs/DOC-16810">http://community.jboss.org/docs/DOC-16810</a> for the full
 * list of currently reserved WFLY message id blocks.
 * <p/>
 *
 * @author Eduardo Martins
 */
@MessageLogger(projectCode = "WFLY")
public interface ConcurrentLogger extends BasicLogger {

    /**
     * A logger with a category of the package name.
     */
    ConcurrentLogger ROOT_LOGGER = Logger.getMessageLogger(ConcurrentLogger.class, ConcurrentLogger.class.getPackage().getName());

    @LogMessage(level = DEBUG)
    @Message(id = 99800, value = "Failure in taskSubmitted() callback.")
    void failureInTaskSubmittedCallback(@Cause Throwable t);

    @LogMessage(level = DEBUG)
    @Message(id = 99801, value = "Failure in taskStarting() callback.")
    void failureInTaskStartingCallback(@Cause Throwable t);

    @LogMessage(level = DEBUG)
    @Message(id = 99802, value = "Failure in taskDone() callback.")
    void failureInTaskDoneCallback(@Cause Throwable t);

    @LogMessage(level = DEBUG)
    @Message(id = 99803, value = "Task never submitted, since Trigger's first invocation of getNextRunTime() returned null.")
    void triggerTaskNeverSubmittedDueToNullGetNextRunTime();

    @LogMessage(level = DEBUG)
    @Message(id = 99804, value = "Task never submitted, since first schedule of a run failed.")
    void triggerTaskNeverSubmittedDueToFailureWhenSchedulingNextRun();

    @LogMessage(level = DEBUG)
    @Message(id = 99805, value = "Failure when scheduling next runtime.")
    void failureWhenSchedulingNextRun(@Cause Throwable e);
}
