/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.time.Duration;

/**
 * A static {@link BeanAccessMetaData} implementation for beans that do not expire.
 * @author Paul Ferraro
 */
public enum ImmortalBeanAccessMetaData implements BeanAccessMetaData {
    INSTANCE;

    @Override
    public Duration getLastAccessDuration() {
        return Duration.ZERO;
    }

    @Override
    public void setLastAccessDuration(Duration duration) {
        // Do nothing
    }
}
