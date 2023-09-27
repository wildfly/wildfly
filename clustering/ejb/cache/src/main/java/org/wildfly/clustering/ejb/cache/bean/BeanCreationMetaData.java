/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.time.Instant;

/**
 * Describes the metadata of a cached bean that does not change between invocations/transactions.
 * @author Paul Ferraro
 * @param <K> the bean group identifier type
 */
public interface BeanCreationMetaData<K> {
    String getName();
    K getGroupId();
    Instant getCreationTime();
}
