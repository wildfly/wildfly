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
package org.jboss.as.ee.concurrent.service;

import org.jboss.as.ee.subsystem.ContextServiceResourceDefinition;
import org.jboss.as.ee.subsystem.ManagedExecutorServiceResourceDefinition;
import org.jboss.as.ee.subsystem.ManagedScheduledExecutorServiceResourceDefinition;
import org.jboss.as.ee.subsystem.ManagedThreadFactoryResourceDefinition;
import org.jboss.msc.service.ServiceName;

/**
 * MSC service names for EE's concurrent resources.
 *
 * @author Eduardo Martins
 */
public class ConcurrentServiceNames {

    private ConcurrentServiceNames() {
    }

    public static final ServiceName BASE_SERVICE_NAME = ServiceName.JBOSS.append("concurrent", "ee");

    private static final ServiceName CONTEXT_BASE_SERVICE_NAME = BASE_SERVICE_NAME.append("context");

    public static final ServiceName TRANSACTION_SETUP_PROVIDER_SERVICE_NAME = BASE_SERVICE_NAME.append("tsp");

    public static final ServiceName CONCURRENT_CONTEXT_BASE_SERVICE_NAME = CONTEXT_BASE_SERVICE_NAME.append("config");

    public static final ServiceName HUNG_TASK_PERIODIC_TERMINATION_SERVICE_NAME = BASE_SERVICE_NAME.append("hung-task-periodic-termination");

    /**
     *
     * @param name
     * @return
     * @deprecated Use "org.wildfly.ee.concurrent.context.service" dynamic capability's service name instead.
     */
    @Deprecated
    public static ServiceName getContextServiceServiceName(String name) {
        return ContextServiceResourceDefinition.CAPABILITY.getCapabilityServiceName(name);
    }

    /**
     *
     * @param name
     * @return
     * @deprecated Use "org.wildfly.ee.concurrent.thread-factory" dynamic capability's service name instead.
     */
    @Deprecated
    public static ServiceName getManagedThreadFactoryServiceName(String name) {
        return ManagedThreadFactoryResourceDefinition.CAPABILITY.getCapabilityServiceName(name);
    }

    /**
     *
     * @param name
     * @return
     * @deprecated Use "org.wildfly.ee.concurrent.executor" dynamic capability's service name instead.
     */
    @Deprecated
    public static ServiceName getManagedExecutorServiceServiceName(String name) {
        return ManagedExecutorServiceResourceDefinition.CAPABILITY.getCapabilityServiceName(name);
    }

    /**
     *
     * @param name
     * @return
     * @deprecated Use "org.wildfly.ee.concurrent.scheduled-executor" dynamic capability's service name instead.
     */
    @Deprecated
    public static ServiceName getManagedScheduledExecutorServiceServiceName(String name) {
        return ManagedScheduledExecutorServiceResourceDefinition.CAPABILITY.getCapabilityServiceName(name);
    }

    public static ServiceName getConcurrentContextServiceName(String app, String module, String component) {
        final ServiceName moduleServiceName = CONCURRENT_CONTEXT_BASE_SERVICE_NAME.append(app).append(module);
        if(component == null) {
            return moduleServiceName;
        } else {
            return moduleServiceName.append(component);
        }
    }

}
