/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import org.wildfly.clustering.cache.Key;

/**
 * The key used to cache the creation metadata of a bean.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 */
public interface BeanMetaDataKey<K> extends Key<K> {

}
