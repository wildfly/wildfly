/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import org.wildfly.clustering.cache.Key;

/**
 * The key used to cache a group of beans.
 * @author Paul Ferraro
 * @param <K> the bean group identifier type
 */
public interface BeanGroupKey<K> extends Key<K> {
}
