/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.microprofile.reactive.messaging.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This uses the maximum value for {@link #getOrdinal()} possible, and thus values here cannot be
 * overridden via other means
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ReactiveMessagingLockedValuesConfigSource extends AbstractReactiveMessagingConfigSource {

    static final Map<String, String> PROPERTIES = new ConcurrentHashMap<>();

    private static final int ORDINAL = Integer.MAX_VALUE;

    public ReactiveMessagingLockedValuesConfigSource() {
        super(ORDINAL, PROPERTIES);
    }

    public static void init() {
        PROPERTIES.clear();
        PROPERTIES.put("smallrye-messaging-strict-binding", "true");
    }
}
