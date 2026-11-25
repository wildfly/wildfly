/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.time.Instant;

import org.wildfly.clustering.server.offset.Value;

/**
 * Bean metadata cache entry.
 * @author Paul Ferraro
 * @param <K> the bean group identifier type
 */
public interface BeanMetaDataEntry<K> extends ImmutableBeanMetaDataEntry<K> {
    @Override
    Value<Instant> getLastAccessTime();
}
