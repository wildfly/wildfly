/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.clustering.controller.Attribute;

/**
 * @author Paul Ferraro
 */
public interface ScheduledThreadPoolDefinition {

    Attribute getMinThreads();

    Attribute getKeepAliveTime();
}
