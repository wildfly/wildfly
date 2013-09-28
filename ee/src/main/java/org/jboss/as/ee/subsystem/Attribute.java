/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.subsystem;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of attributes used in the EE subsystem.
 *
 * @author John E. Bailey
 */
enum Attribute {
    UNKNOWN(null),
    NAME(GlobalModulesDefinition.NAME),
    SLOT(GlobalModulesDefinition.SLOT),
    ANNOTATIONS(GlobalModulesDefinition.ANNOTATIONS),
    SERVICES(GlobalModulesDefinition.SERVICES),
    META_INF(GlobalModulesDefinition.META_INF),

    // from ee concurrent
    JNDI_NAME(ContextServiceResourceDefinition.JNDI_NAME),
    USE_TRANSACTION_SETUP_PROVIDER(ContextServiceResourceDefinition.USE_TRANSACTION_SETUP_PROVIDER),
    CONTEXT_SERVICE(ManagedThreadFactoryResourceDefinition.CONTEXT_SERVICE),
    PRIORITY(ManagedThreadFactoryResourceDefinition.PRIORITY),
    THREAD_FACTORY(ManagedExecutorServiceResourceDefinition.THREAD_FACTORY),
    HUNG_TASK_THRESHOLD(ManagedExecutorServiceResourceDefinition.HUNG_TASK_THRESHOLD),
    LONG_RUNNING_TASKS(ManagedExecutorServiceResourceDefinition.LONG_RUNNING_TASKS),
    CORE_THREADS(ManagedExecutorServiceResourceDefinition.CORE_THREADS),
    MAX_THREADS(ManagedExecutorServiceResourceDefinition.MAX_THREADS),
    KEEPALIVE_TIME(ManagedExecutorServiceResourceDefinition.KEEPALIVE_TIME),
    QUEUE_LENGTH(ManagedExecutorServiceResourceDefinition.QUEUE_LENGTH),
    REJECT_POLICY(ManagedExecutorServiceResourceDefinition.REJECT_POLICY),

    DATASOURCE(DefaultBindingsResourceDefinition.DATASOURCE),
    JMS_CONNECTION_FACTORY(DefaultBindingsResourceDefinition.JMS_CONNECTION_FACTORY),
    MANAGED_EXECUTOR_SERVICE(DefaultBindingsResourceDefinition.MANAGED_EXECUTOR_SERVICE),
    MANAGED_SCHEDULED_EXECUTOR_SERVICE(DefaultBindingsResourceDefinition.MANAGED_SCHEDULED_EXECUTOR_SERVICE),
    MANAGED_THREAD_FACTORY(DefaultBindingsResourceDefinition.MANAGED_THREAD_FACTORY)
    ;

    private final String name;

    Attribute(final String name) {
        this.name = name;
    }

    /**
     * Get the local name of this attribute.
     *
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    private static final Map<String, Attribute> MAP;

    static {
        final Map<String, Attribute> map = new HashMap<String, Attribute>();
        for (Attribute element : values()) {
            final String name = element.getLocalName();
            if (name != null) map.put(name, element);
        }
        MAP = map;
    }

    public static Attribute forName(String localName) {
        final Attribute element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }

    public String toString() {
        return getLocalName();
    }
}
