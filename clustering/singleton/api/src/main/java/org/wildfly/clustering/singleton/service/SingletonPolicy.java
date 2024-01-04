/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.service;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * Defines a policy for creating singleton services.
 * @author Paul Ferraro
 */
public interface SingletonPolicy {

    ServiceConfigurator createSingletonServiceConfigurator(ServiceName name);
}
