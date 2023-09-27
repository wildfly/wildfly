/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.bean;

import java.time.Instant;

/**
 * Described the mutable and immutable metadata of a cached bean.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 */
public interface BeanMetaData<K> extends ImmutableBeanMetaData<K> {

    void setLastAccessTime(Instant lastAccessTime);
}
