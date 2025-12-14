/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.time.Instant;
import java.util.function.Supplier;

import org.wildfly.clustering.cache.function.Remappable;
import org.wildfly.clustering.server.offset.Offset;
import org.wildfly.clustering.server.offset.OffsetValue;

/**
 * Bean metadata cache entry, supporting remapping.
 * @author Paul Ferraro
 * @param <K> the bean group identifier type
 */
public interface RemappableBeanMetaDataEntry<K> extends BeanMetaDataEntry<K>, Remappable<RemappableBeanMetaDataEntry<K>, Supplier<Offset<Instant>>> {
    @Override
    OffsetValue<Instant> getLastAccessTime();
}
