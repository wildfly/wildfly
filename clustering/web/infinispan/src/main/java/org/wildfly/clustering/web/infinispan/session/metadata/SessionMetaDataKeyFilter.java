/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.session.metadata;


import org.infinispan.util.function.SerializablePredicate;

/**
 * Filters a cache for session meta data entries.
 * @author Paul Ferraro
 */
public enum SessionMetaDataKeyFilter implements SerializablePredicate<Object> {
    INSTANCE;

    @Override
    public boolean test(Object key) {
        return key instanceof SessionMetaDataKey;
    }
}
