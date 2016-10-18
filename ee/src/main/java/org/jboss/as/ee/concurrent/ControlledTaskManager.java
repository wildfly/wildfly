/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.as.ee.concurrent;

import java.util.concurrent.Executor;

import org.wildfly.extension.requestcontroller.ControlPoint;

/**
 * Allows access to the {@link ControlPoint} for this task manager. Tasks submitted via {@link Executor#execute(Runnable)}
 * may be queued if the server {@linkplain #isSuspended() suspended}. When the server resumes any queued tasks will be
 * executed.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public interface ControlledTaskManager {

    /**
     * Returns the {@link ControlPoint} used or {@code null} if request controlling is not required.
     *
     * @return the {@code ControlPoint} to use or {@code null}
     */
    ControlPoint getControlPoint();

    /**
     * Submits a runnable to the executor. If the server {@linkplain #isSuspended() is suspended} the task will be
     * queued and ran when the server has resumed.
     *
     * @param task the task to submit
     */
    void submitTask(Runnable task, Executor executor);

    /**
     * Indicates whether or not the server has been suspended.
     *
     * @return {@code true} if the server is suspended, or suspending, otherwise {@code false}
     */
    boolean isSuspended();
}