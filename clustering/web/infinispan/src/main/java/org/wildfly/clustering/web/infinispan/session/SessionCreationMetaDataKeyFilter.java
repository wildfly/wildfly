/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.session;


import org.infinispan.util.function.SerializablePredicate;

/**
 * Filters a cache for session creation meta data entries.
 * @author Paul Ferraro
 */
public enum SessionCreationMetaDataKeyFilter implements SerializablePredicate<Object> {
    INSTANCE;

    @Override
    public boolean test(Object key) {
        return key instanceof SessionCreationMetaDataKey;
    }
}
