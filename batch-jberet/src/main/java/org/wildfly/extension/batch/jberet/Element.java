/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public enum Element {

    UNKNOWN(null),
    DEFAULT_JOB_REPOSITORY("default-job-repository"),
    DEFAULT_THREAD_POOL("default-thread-pool"),
    JOB_REPOSITORY("job-repository"),
    JDBC("jdbc"),
    IN_MEMORY("in-memory"),
    NAMED("named"),
    RESTART_JOBS_ON_RESUME("restart-jobs-on-resume"),
    SECURITY_DOMAIN("security-domain"),
    THREAD_FACTORY("thread-factory"),
    THREAD_POOL("thread-pool"),
    ;

    private final String name;

    Element(final String name) {
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

    @Override
    public String toString() {
        return name;
    }

    private static final Map<String, Element> MAP;

    static {
        final Map<String, Element> map = new HashMap<>();
        for (Element element : values()) {
            final String name = element.getLocalName();
            if (name != null) map.put(name, element);
        }
        MAP = map;
    }

    public static Element forName(String localName) {
        final Element element = MAP.get(localName);
        return element == null ? UNKNOWN : element;
    }
}
