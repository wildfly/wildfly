/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.undertow.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public enum LocalSessionContextFactory implements Supplier<Map<String, Object>> {
    INSTANCE;

    @Override
    public Map<String, Object> get() {
        return new ConcurrentHashMap<>();
    }
}
