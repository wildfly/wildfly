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
 * MSC service names for EE's concurrent resources.
 *
 * @author Eduardo Martins
 */
public class ConcurrentServiceNames {

    private ConcurrentServiceNames() {
    }

    public static final String DEFAULT_NAME = "";

    public static final String BASE_JNDI_NAME = "java:jboss/ee/concurrent/";

    public static final String CONTEXT_SERVICE_BASE_JNDI_NAME = BASE_JNDI_NAME + "context/";

    public static final String DEFAULT_CONTEXT_SERVICE_JNDI_NAME = BASE_JNDI_NAME + "default/context";

    private static final String MANAGED_THREAD_FACTORY_BASE_JNDI_NAME = BASE_JNDI_NAME + "threadfactory/";

    public static final String DEFAULT_MANAGED_THREAD_FACTORY_JNDI_NAME = BASE_JNDI_NAME + "default/threadfactory";

    private static final String MANAGED_EXECUTOR_SERVICE_BASE_JNDI_NAME = BASE_JNDI_NAME + "executor/";

    public static final String DEFAULT_MANAGED_EXECUTOR_SERVICE_JNDI_NAME = BASE_JNDI_NAME + "default/executor";

    private static final String MANAGED_SCHEDULED_EXECUTOR_SERVICE_BASE_JNDI_NAME = BASE_JNDI_NAME + "scheduledexecutor/";

    public static final String DEFAULT_MANAGED_SCHEDULED_EXECUTOR_SERVICE_JNDI_NAME = BASE_JNDI_NAME + "default/scheduledexecutor";

    public static final ServiceName BASE_SERVICE_NAME = ServiceName.JBOSS.append("concurrent", "ee");

    private static final ServiceName CONTEXT_BASE_SERVICE_NAME = BASE_SERVICE_NAME.append("context");

    private static final ServiceName CONTEXT_SERVICE_BASE_SERVICE_NAME = CONTEXT_BASE_SERVICE_NAME.append("service");

    public static final ServiceName DEFAULT_CONTEXT_SERVICE_SERVICE_NAME = CONTEXT_BASE_SERVICE_NAME.append("defaultservice");

    private static final ServiceName MANAGED_THREAD_FACTORY_BASE_SERVICE_NAME = BASE_SERVICE_NAME.append("threadfactory");

    public static final ServiceName DEFAULT_MANAGED_THREAD_FACTORY_SERVICE_NAME = BASE_SERVICE_NAME.append("default").append("threadfactory");

    private static final ServiceName MANAGED_EXECUTOR_SERVICE_BASE_SERVICE_NAME = BASE_SERVICE_NAME.append("executor");

    public static final ServiceName DEFAULT_MANAGED_EXECUTOR_SERVICE_SERVICE_NAME = BASE_SERVICE_NAME.append("default").append("executor");

    private static final ServiceName MANAGED_SCHEDULED_EXECUTOR_SERVICE_BASE_SERVICE_NAME = BASE_SERVICE_NAME.append("scheduledexecutor");

    public static final ServiceName DEFAULT_MANAGED_SCHEDULED_EXECUTOR_SERVICE_SERVICE_NAME = BASE_SERVICE_NAME.append("default").append("scheduledexecutor");

    public static final ServiceName TRANSACTION_SETUP_PROVIDER_SERVICE_NAME = BASE_SERVICE_NAME.append("tsp");

    public static final ServiceName CONCURRENT_CONTEXT_BASE_SERVICE_NAME = CONTEXT_BASE_SERVICE_NAME.append("config");

    public static String getContextServiceJndiName(String name) {
        return DEFAULT_NAME.equals(name) ? DEFAULT_CONTEXT_SERVICE_JNDI_NAME :  CONTEXT_SERVICE_BASE_JNDI_NAME + name;
    }

    public static String getManagedThreadFactoryJndiName(String name) {
        return DEFAULT_NAME.equals(name) ? DEFAULT_MANAGED_THREAD_FACTORY_JNDI_NAME :  MANAGED_THREAD_FACTORY_BASE_JNDI_NAME + name;
    }

    public static String getManagedExecutorServiceJndiName(String name) {
        return DEFAULT_NAME.equals(name) ? DEFAULT_MANAGED_EXECUTOR_SERVICE_JNDI_NAME : MANAGED_EXECUTOR_SERVICE_BASE_JNDI_NAME + name;
    }

    public static String getManagedScheduledExecutorServiceJndiName(String name) {
        return DEFAULT_NAME.equals(name) ? DEFAULT_MANAGED_SCHEDULED_EXECUTOR_SERVICE_JNDI_NAME :  MANAGED_SCHEDULED_EXECUTOR_SERVICE_BASE_JNDI_NAME + name;
    }

    public static ServiceName getContextServiceServiceName(String name) {
        return DEFAULT_NAME.equals(name) ? DEFAULT_CONTEXT_SERVICE_SERVICE_NAME :  CONTEXT_SERVICE_BASE_SERVICE_NAME.append(name);
    }

    public static ServiceName getManagedThreadFactoryServiceName(String name) {
        return DEFAULT_NAME.equals(name) ? DEFAULT_MANAGED_THREAD_FACTORY_SERVICE_NAME :  MANAGED_THREAD_FACTORY_BASE_SERVICE_NAME.append(name);
    }

    public static ServiceName getManagedExecutorServiceServiceName(String name) {
        return DEFAULT_NAME.equals(name) ? DEFAULT_MANAGED_EXECUTOR_SERVICE_SERVICE_NAME :  MANAGED_EXECUTOR_SERVICE_BASE_SERVICE_NAME.append(name);
    }

    public static ServiceName getManagedScheduledExecutorServiceServiceName(String name) {
        return DEFAULT_NAME.equals(name) ? DEFAULT_MANAGED_SCHEDULED_EXECUTOR_SERVICE_SERVICE_NAME :  MANAGED_SCHEDULED_EXECUTOR_SERVICE_BASE_SERVICE_NAME.append(name);
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
