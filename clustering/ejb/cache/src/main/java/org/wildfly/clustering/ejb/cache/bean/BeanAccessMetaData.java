/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.time.Duration;

/**
 * Describes the metadata of a cached bean that changes per invocation/transaction.
 * @author Paul Ferraro
 */
public interface BeanAccessMetaData extends ImmutableBeanAccessMetaData {

    /**
     * Sets the duration of time since creation for this bean.
     * @param duration the duration of time since bean creation.
     */
    void setLastAccessDuration(Duration duration);
}
