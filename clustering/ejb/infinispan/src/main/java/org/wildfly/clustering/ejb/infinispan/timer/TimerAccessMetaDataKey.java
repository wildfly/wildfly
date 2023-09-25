/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import org.wildfly.clustering.ee.infinispan.GroupedKey;

/**
 * @author Paul Ferraro
 */
public class TimerAccessMetaDataKey<I> extends GroupedKey<I> {

    public TimerAccessMetaDataKey(I id) {
        super(id);
    }
}
