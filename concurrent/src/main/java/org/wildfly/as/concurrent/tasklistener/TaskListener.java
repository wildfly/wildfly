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

package org.wildfly.as.concurrent.tasklistener;

import java.util.concurrent.Future;

/**
 * <p>Listener for callbacks with respect to task execution.</p>
 * <p>Possible scenarios:</p>
 * <ul>
 * <li>Task submit failed to complete - taskSubmitted, taskDone(Throwable which aborted the submit)</li>
 * <li>Task execution aborted - taskSubmitted, taskStarting, taskDone(Throwable which aborted execution)</li>
 * <li>Task cancellation before execution - taskSubmitted, taskDone(CancellationException)</li>
 * <li>Task cancellation during execution - taskSubmitted, taskStarting, taskDone(CancellationException)</li>
 * <li>Task successfully executed - taskSubmitted, taskStarting, taskDone(null)</li>
 * </ul>
 * <p>Periodic tasks will produce one scenario per run.</p>
 *
 * @author Eduardo Martins
 */
public interface TaskListener {

    /**
     * Callback which indicates the task has been submitted.
     *
     * @param future
     */
    void taskSubmitted(Future<?> future);

    /**
     * Callback which indicates the task execution is starting.
     */
    void taskStarting();

    /**
     * Callback which indicates the task is done.
     *
     * @param exception null if the task executed successfully; CancellationException if the task has been cancelled; any other non null Throwable is related to an aborted task submit or execution.
     */
    void taskDone(Throwable exception);
}
