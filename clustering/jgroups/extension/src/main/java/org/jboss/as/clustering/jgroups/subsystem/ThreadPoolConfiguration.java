/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

/**
 * @author Paul Ferraro
 */
public interface ThreadPoolConfiguration {
    int getMinThreads();
    int getMaxThreads();
    long getKeepAliveTime();
}
