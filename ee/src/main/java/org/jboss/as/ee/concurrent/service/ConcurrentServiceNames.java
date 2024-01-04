/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
