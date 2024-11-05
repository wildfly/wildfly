/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.subsystem;

import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of concurrency elements used in a subsystem
 *
 * @author emartins
 */
enum ConcurrentElement {
    CONCURRENT("concurrent"),
    CONTEXT_SERVICES("context-services"),
    CONTEXT_SERVICE("context-service"),
    MANAGED_THREAD_FACTORIES("managed-thread-factories"),
    MANAGED_THREAD_FACTORY("managed-thread-factory"),
    MANAGED_EXECUTOR_SERVICES("managed-executor-services"),
    MANAGED_EXECUTOR_SERVICE("managed-executor-service"),
    MANAGED_SCHEDULED_EXECUTOR_SERVICES("managed-scheduled-executor-services"),
    MANAGED_SCHEDULED_EXECUTOR_SERVICE("managed-scheduled-executor-service"),

    UNKNOWN(null);

    private final String name;

    ConcurrentElement(final String name) {
        this.name = name;
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }

    private static final Map<String, ConcurrentElement> MAP;

    static {
        final Map<String, ConcurrentElement> map = new HashMap<String, ConcurrentElement>();
        for (ConcurrentElement element : values()) {
            final String name = element.getLocalName();
            if (name != null) map.put(name, element);
        }
        MAP = map;
    }

    public static ConcurrentElement forName(String localName) {
        final ConcurrentElement element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }
}
