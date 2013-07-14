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

import org.jboss.msc.service.ServiceName;

/**
 * @author Eduardo Martins
 */
public class ConcurrentServiceNames {

    private ConcurrentServiceNames() {
    }

    static final ServiceName BASE_SERVICE_NAME = ServiceName.JBOSS.append("concurrent", "ee");

    static final ServiceName CONTEXT_SERVICE_BASE_SERVICE_NAME = BASE_SERVICE_NAME.append("contextservice");

    static final ServiceName MANAGED_THREAD_FACTORY_BASE_SERVICE_NAME = BASE_SERVICE_NAME.append("threadfactory");

    static final ServiceName EXECUTOR_SERVICE_BASE_SERVICE_NAME = BASE_SERVICE_NAME.append("executor");

    static final ServiceName MANAGED_EXECUTOR_SERVICE_BASE_SERVICE_NAME = EXECUTOR_SERVICE_BASE_SERVICE_NAME.append("managed");

    static final ServiceName MANAGED_SCHEDULED_EXECUTOR_SERVICE_BASE_SERVICE_NAME = EXECUTOR_SERVICE_BASE_SERVICE_NAME.append("managedscheduled");

    static final ServiceName TASK_DECORATOR_EXECUTOR_SERVICE_BASE_SERVICE_NAME = EXECUTOR_SERVICE_BASE_SERVICE_NAME.append("decorator");

    static final ServiceName TASK_DECORATOR_SCHEDULED_EXECUTOR_SERVICE_BASE_SERVICE_NAME = EXECUTOR_SERVICE_BASE_SERVICE_NAME.append("scheduleddecorator");

    public static ServiceName getContextServiceServiceName(String app, String module, String comp, String jndiNameRelativeToJavaComp) {
        return CONTEXT_SERVICE_BASE_SERVICE_NAME.append(app).append(module).append(comp).append(jndiNameRelativeToJavaComp);
    }

    public static ServiceName getDefaultContextServiceServiceName(String app, String module, String comp) {
        return getContextServiceServiceName(app, module, comp, "DefaultContextService");
    }

    public static ServiceName getManagedThreadFactoryServiceName(String app, String module, String comp, String jndiNameRelativeToJavaComp) {
        return MANAGED_THREAD_FACTORY_BASE_SERVICE_NAME.append(app).append(module).append(comp).append(jndiNameRelativeToJavaComp);
    }

    public static ServiceName getDefaultManagedThreadFactoryServiceName(String app, String module, String comp) {
        return getManagedThreadFactoryServiceName(app, module, comp, "DefaultManagedThreadFactory");
    }

    public static ServiceName getManagedExecutorServiceServiceName(String app, String module, String comp, String jndiNameRelativeToJavaComp) {
        return MANAGED_EXECUTOR_SERVICE_BASE_SERVICE_NAME.append(app).append(module).append(comp).append(jndiNameRelativeToJavaComp);
    }

    public static ServiceName getDefaultManagedExecutorServiceServiceName(String app, String module, String comp) {
        return getManagedExecutorServiceServiceName(app, module, comp, "DefaultManagedExecutorService");
    }

    public static ServiceName getManagedScheduledExecutorServiceServiceName(String app, String module, String comp, String jndiNameRelativeToJavaComp) {
        return MANAGED_SCHEDULED_EXECUTOR_SERVICE_BASE_SERVICE_NAME.append(app).append(module).append(comp).append(jndiNameRelativeToJavaComp);
    }

    public static ServiceName getDefaultManagedScheduledExecutorServiceServiceName(String app, String module, String comp) {
        return getManagedScheduledExecutorServiceServiceName(app, module, comp, "DefaultManagedScheduledExecutorService");
    }

    public static ServiceName getTaskDecoratorExecutorServiceServiceName(String name) {
        return TASK_DECORATOR_EXECUTOR_SERVICE_BASE_SERVICE_NAME.append("custom").append(name);
    }

    public static ServiceName getDefaultTaskDecoratorExecutorServiceServiceName() {
        return TASK_DECORATOR_EXECUTOR_SERVICE_BASE_SERVICE_NAME.append("default");
    }

    public static ServiceName getTaskDecoratorScheduledExecutorServiceServiceName(String name) {
        return TASK_DECORATOR_SCHEDULED_EXECUTOR_SERVICE_BASE_SERVICE_NAME.append("custom").append(name);
    }

    public static ServiceName getDefaultTaskDecoratorScheduledExecutorServiceServiceName() {
        return TASK_DECORATOR_SCHEDULED_EXECUTOR_SERVICE_BASE_SERVICE_NAME.append("default");
    }

}
