/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.mod_cluster;

import java.time.Duration;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;

/**
 * Creates builder of a service that triggers container events for use by {@link org.jboss.modcluster.container.ContainerEventHandler}.
 *
 * @author Paul Ferraro
 * @author Radoslav Husar
 */
public interface ContainerEventHandlerAdapterServiceConfiguratorProvider {
    CapabilityServiceConfigurator getServiceConfigurator(String adapterName, String listenerName, Duration statusInterval);
}
