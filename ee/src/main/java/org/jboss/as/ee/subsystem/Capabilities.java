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

package org.jboss.as.ee.subsystem;

import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.ee.concurrent.ControlledTaskManager;

/**
 * Capability references for the EE subsystem.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class Capabilities {

    /**
     * The request controller capability name
     */
    static final String REQUEST_CONTROLLER_CAPABILITY_NAME = "org.wildfly.request-controller";

    /**
     * The capability for the {@link ControlledTaskManager} for a {@link javax.enterprise.concurrent.ManagedExecutorService}
     */
    static final RuntimeCapability<Void> CONTROLLED_TASK_MANAGER_CAPABILITY = RuntimeCapability.Builder
            .of("org.wildfly.ee.concurrent.task.manager", true, ControlledTaskManager.class)
            .build();

    /**
     * The capability for the {@link ControlledTaskManager} for a {@link javax.enterprise.concurrent.ManagedScheduledExecutorService}
     */
    static final RuntimeCapability<Void> CONTROLLED_SCHEDULED_TASK_MANAGER_CAPABILITY = RuntimeCapability.Builder
            .of("org.wildfly.ee.concurrent.scheduled.task.manager", true, ControlledTaskManager.class)
            .build();
}