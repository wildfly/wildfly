/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ee.subsystem;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of Concurrency attributes used in a subsystem.
 *
 * @author emartins
 */
enum ConcurrentAttribute {
    UNKNOWN(null),
    NAME("name"),
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
    ;

    private final String name;

    ConcurrentAttribute(final String name) {
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

    private static final Map<String, ConcurrentAttribute> MAP;

    static {
        final Map<String, ConcurrentAttribute> map = new HashMap<String, ConcurrentAttribute>();
        for (ConcurrentAttribute element : values()) {
            final String name = element.getLocalName();
            if (name != null) map.put(name, element);
        }
        MAP = map;
    }

    public static ConcurrentAttribute forName(String localName) {
        final ConcurrentAttribute element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }

    public String toString() {
        return getLocalName();
    }
}
