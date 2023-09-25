/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.cache.bean;

import java.time.Duration;

/**
 * An immutable description of the metadata of a cached bean that changes per invocation/transaction.
 * @author Paul Ferraro
 */
public interface ImmutableBeanAccessMetaData {

    /**
     * Returns the duration of time between bean creation and  last access.
     * @return
     */
    Duration getLastAccessDuration();
}
