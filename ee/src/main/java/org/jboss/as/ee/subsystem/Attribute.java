/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.subsystem;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.domain.management.ModelDescriptionConstants;

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
    THREAD_PRIORITY(ManagedExecutorServiceResourceDefinition.THREAD_PRIORITY),
    HUNG_TASK_TERMINATION_PERIOD(ManagedExecutorServiceResourceDefinition.HUNG_TASK_TERMINATION_PERIOD),
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
    MANAGED_THREAD_FACTORY(DefaultBindingsResourceDefinition.MANAGED_THREAD_FACTORY),
    PATH(ModelDescriptionConstants.PATH),
    RELATIVE_TO(ModelDescriptionConstants.RELATIVE_TO)
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
