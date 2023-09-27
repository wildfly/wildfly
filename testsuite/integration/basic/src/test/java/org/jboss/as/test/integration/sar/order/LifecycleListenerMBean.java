/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.order;

import java.util.List;

public interface LifecycleListenerMBean {

    void mbeanCreated(String id);

    void mbeanStarted(String id);

    void mbeanStopped(String id);

    void mbeanDestroyed(String id);

    List<String> getCreates();

    List<String> getStarts();

    List<String> getStops();

    List<String> getDestroys();

}
