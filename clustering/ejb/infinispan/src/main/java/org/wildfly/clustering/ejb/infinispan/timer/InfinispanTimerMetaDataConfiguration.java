/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import org.wildfly.clustering.cache.infinispan.embedded.EmbeddedCacheConfiguration;
import org.wildfly.clustering.ejb.cache.timer.TimerMetaDataConfiguration;

/**
 * @author Paul Ferraro
 * @param <C> the timer context type
 */
public interface InfinispanTimerMetaDataConfiguration<C> extends TimerMetaDataConfiguration<C>, EmbeddedCacheConfiguration {
}
