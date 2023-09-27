/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.infinispan.bean;

import java.util.function.Supplier;

import org.infinispan.commons.api.BasicCache;

/**
 * @author Paul Ferraro
 */
public abstract class CacheBean implements Cache, Supplier<BasicCache<Key, Value>> {

    @Override
    public String get(String key) {
        Value value = this.get().get(new Key(key));
        return (value != null) ? value.getValue() : null;
    }

    @Override
    public String put(String key, String value) {
        Value old = this.get().put(new Key(key), new Value(value));
        return (old != null) ? old.getValue() : null;
    }

    @Override
    public String remove(String key) {
        Value old = this.get().remove(new Key(key));
        return (old != null) ? old.getValue() : null;
    }
}
