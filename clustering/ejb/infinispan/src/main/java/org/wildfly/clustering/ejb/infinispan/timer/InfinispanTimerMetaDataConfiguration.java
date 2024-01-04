/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import org.wildfly.clustering.ee.infinispan.InfinispanConfiguration;
import org.wildfly.clustering.ejb.cache.timer.TimerMetaDataConfiguration;

/**
 * @author Paul Ferraro
 */
public interface InfinispanTimerMetaDataConfiguration<C> extends TimerMetaDataConfiguration<C>, InfinispanConfiguration {
}
