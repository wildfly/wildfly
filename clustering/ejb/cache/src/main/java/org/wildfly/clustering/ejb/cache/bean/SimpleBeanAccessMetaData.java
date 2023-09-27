/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.time.Duration;

/**
 * A simple {@link BeanAccessMetaData} implementation.
 * @author Paul Ferraro
 */
public class SimpleBeanAccessMetaData implements BeanAccessMetaData {

    static SimpleBeanAccessMetaData valueOf(Duration duration) {
        SimpleBeanAccessMetaData metaData = new SimpleBeanAccessMetaData();
        metaData.setLastAccessDuration(duration);
        return metaData;
    }

    private volatile Duration lastAccessDuration = Duration.ZERO;

    @Override
    public Duration getLastAccessDuration() {
        return this.lastAccessDuration;
    }

    @Override
    public void setLastAccessDuration(Duration duration) {
        this.lastAccessDuration = duration;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(this.getClass().getSimpleName()).append(" { ");
        builder.append("last-accessed = ").append(this.lastAccessDuration);
        return builder.append(" }").toString();
    }
}
