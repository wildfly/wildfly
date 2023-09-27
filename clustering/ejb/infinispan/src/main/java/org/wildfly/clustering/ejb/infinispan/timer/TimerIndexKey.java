/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import org.wildfly.clustering.ee.infinispan.GroupedKey;

/**
 * @author Paul Ferraro
 */
public class TimerIndexKey extends GroupedKey<TimerIndex> {

    public TimerIndexKey(TimerIndex index) {
        super(index);
    }
}
