/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.web.undertow.session;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.wildfly.clustering.web.LocalContextFactory;

public enum LocalSessionContextFactory implements LocalContextFactory<Map<String, Object>> {
    INSTANCE;

    @Override
    public Map<String, Object> createLocalContext() {
        return new ConcurrentHashMap<>();
    }
}
