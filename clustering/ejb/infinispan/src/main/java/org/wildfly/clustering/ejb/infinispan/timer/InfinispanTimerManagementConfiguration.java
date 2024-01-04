/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import org.wildfly.clustering.ee.infinispan.InfinispanCacheConfiguration;
import org.wildfly.clustering.ejb.timer.TimerManagementConfiguration;

/**
 * @author Paul Ferraro
 */
public interface InfinispanTimerManagementConfiguration extends TimerManagementConfiguration, InfinispanCacheConfiguration {

}
