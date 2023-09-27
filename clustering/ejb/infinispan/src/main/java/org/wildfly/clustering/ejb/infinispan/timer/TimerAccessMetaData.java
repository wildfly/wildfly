/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.infinispan.timer;

import java.time.Duration;

/**
 * @author Paul Ferraro
 */
public interface TimerAccessMetaData {

    Duration getLastTimout();
    void setLastTimeout(Duration sinceCreation);
}
