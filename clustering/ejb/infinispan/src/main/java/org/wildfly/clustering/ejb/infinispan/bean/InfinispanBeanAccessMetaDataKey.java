/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.bean;

import org.wildfly.clustering.ee.infinispan.GroupedKey;
import org.wildfly.clustering.ejb.cache.bean.BeanAccessMetaDataKey;

/**
 * The key used to cache the access metadata of a bean.
 * @author Paul Ferraro
 * @param <K> the bean identifier type
 */
public class InfinispanBeanAccessMetaDataKey<K> extends GroupedKey<K> implements BeanAccessMetaDataKey<K> {

    public InfinispanBeanAccessMetaDataKey(K id) {
        super(id);
    }
}
