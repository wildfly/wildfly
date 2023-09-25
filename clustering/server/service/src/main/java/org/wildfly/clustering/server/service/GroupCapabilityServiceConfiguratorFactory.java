/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.clustering.server.service;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.msc.service.ServiceName;

/**
 * Builds a group-based service.
 * @author Paul Ferraro
 */
public interface GroupCapabilityServiceConfiguratorFactory<T> {
    CapabilityServiceConfigurator createServiceConfigurator(ServiceName name, String group);
}
