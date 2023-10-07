/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.time.Instant;

import org.wildfly.clustering.ejb.bean.BeanExpiration;
import org.wildfly.clustering.ejb.bean.BeanMetaData;

/**
 * @author Paul Ferraro
 */
public class ImmortalBeanMetaData<K> extends DefaultImmutableBeanMetaData<K> implements BeanMetaData<K> {

    public ImmortalBeanMetaData(ImmutableBeanMetaDataEntry<K> entry, BeanExpiration expiration) {
        super(entry, expiration);
    }

    @Override
    public void setLastAccessTime(Instant lastAccessTime) {
        // Ignore
    }

    @Override
    public void close() {
        // Do nothing
    }
}
