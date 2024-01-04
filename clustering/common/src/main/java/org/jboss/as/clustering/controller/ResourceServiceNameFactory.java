/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.PathAddress;
import org.jboss.msc.service.ServiceName;

/**
 * Generates the {@link ServiceName} for a resource service.
 * @author Paul Ferraro
 */
public interface ResourceServiceNameFactory {
    /**
     * Returns {@link ServiceName} for the specified resource address.
     * @param address a resource address
     * @return a server name
     */
    ServiceName getServiceName(PathAddress address);
}
