/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.clustering.controller.Attribute;

/**
 * @author Paul Ferraro
 */
public interface ThreadPoolDefinition {
    Attribute getMinThreads();
    Attribute getMaxThreads();
    Attribute getKeepAliveTime();
}
