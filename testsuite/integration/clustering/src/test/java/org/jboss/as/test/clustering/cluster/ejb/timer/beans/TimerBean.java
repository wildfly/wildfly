/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.clustering.cluster.ejb.timer.beans;

import java.time.Instant;
import java.util.List;

/**
 * @author Paul Ferraro
 */
public interface TimerBean {

    String getNodeName();

    boolean isCoordinator();

    List<Instant> getTimeouts();

    int getTimers();
}
