/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.clustering.controller.ResourceServiceNameFactory;

/**
 * @author Paul Ferraro
 */
public interface ScheduledThreadPoolDefinition extends ResourceServiceNameFactory {

    Attribute getMinThreads();

    Attribute getKeepAliveTime();
}
