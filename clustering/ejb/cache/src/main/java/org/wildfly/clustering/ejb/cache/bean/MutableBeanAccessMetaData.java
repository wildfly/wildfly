/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.time.Duration;

import org.wildfly.clustering.ee.Mutator;

/**
 * A {@link BeanAccessMetaData} implementation that triggers a mutator on modification.
 * @author Paul Ferraro
 */
public class MutableBeanAccessMetaData implements BeanAccessMetaData {

    private final BeanAccessMetaData metaData;
    private final Mutator mutator;

    public MutableBeanAccessMetaData(BeanAccessMetaData metaData, Mutator mutator) {
        this.metaData = metaData;
        this.mutator = mutator;
    }

    @Override
    public Duration getLastAccessDuration() {
        return this.metaData.getLastAccessDuration();
    }

    @Override
    public void setLastAccessDuration(Duration duration) {
        this.metaData.setLastAccessDuration(duration);
        this.mutator.mutate();
    }
}
