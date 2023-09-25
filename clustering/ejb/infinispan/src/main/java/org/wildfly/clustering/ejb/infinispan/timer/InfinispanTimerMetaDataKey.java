/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import org.wildfly.clustering.ee.infinispan.GroupedKey;

/**
 * Encapsulates the meta data key for a timer
 * @author Paul Ferraro
 */
public class InfinispanTimerMetaDataKey<I> extends GroupedKey<I> implements org.wildfly.clustering.ejb.cache.timer.TimerMetaDataKey<I> {

    public InfinispanTimerMetaDataKey(I id) {
        super(id);
    }
}
