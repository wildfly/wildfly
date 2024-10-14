/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.reactive.messaging.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This uses a value for {@link #getOrdinal()} of {@code 80} which means values can be overridden by
 * system properties, environment variables, and properties in META-INF/microprofile-config.properties (if they use
 * the default ordinal of {@code 100})
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ReactiveMessagingDefaultValuesConfigSource extends AbstractReactiveMessagingConfigSource {
    static final Map<String, String> PROPERTIES = new ConcurrentHashMap<>();
    private static final int ORDINAL = 80;

    public ReactiveMessagingDefaultValuesConfigSource() {
        super(ORDINAL, PROPERTIES);
    }
}
