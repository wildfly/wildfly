/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.sar.order;

import javax.management.ObjectName;

public interface LifecycleEmitterMBean {

    String getId();

    void setId(String id);

    ObjectName getLifecycleListener();

    void setLifecycleListener(ObjectName lifecycleListener);
}
