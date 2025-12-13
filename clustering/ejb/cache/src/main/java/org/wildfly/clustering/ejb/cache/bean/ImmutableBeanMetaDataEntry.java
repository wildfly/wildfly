/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.time.Instant;
import java.util.function.Supplier;

/**
 * Immutable view of a bean metadata cache entry.
 * @author Paul Ferraro
 * @param <K> the bean group identifier type
 */
public interface ImmutableBeanMetaDataEntry<K> {
    String getName();
    K getGroupId();
    Supplier<Instant> getLastAccessTime();
}
