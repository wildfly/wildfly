/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet;

import java.util.Map;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
enum Namespace {
    // must be first
    UNKNOWN(null),

    BATCH_1_0("urn:jboss:domain:batch-jberet:1.0"),
    BATCH_2_0("urn:jboss:domain:batch-jberet:2.0"),
    BATCH_3_0("urn:jboss:domain:batch-jberet:3.0"),
    ;

    private static final Map<String, Namespace> MAP = Map.of(
            BATCH_1_0.name, BATCH_1_0,
            BATCH_2_0.name, BATCH_2_0,
            BATCH_3_0.name, BATCH_3_0
    );

    /**
     * The current namespace version.
     */
    public static final Namespace CURRENT = BATCH_3_0;

    private final String name;

    Namespace(final String name) {
        this.name = name;
    }

    /**
     * Get the URI of this namespace.
     *
     * @return the URI
     */
    public String getUriString() {
        return name;
    }

    public static Namespace forUri(String uri) {
        if (uri == null) {
            return UNKNOWN;
        }
        final Namespace element = MAP.get(uri);
        return element == null ? UNKNOWN : element;
    }
}
