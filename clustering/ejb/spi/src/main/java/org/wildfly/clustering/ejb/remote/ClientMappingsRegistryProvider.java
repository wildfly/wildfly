/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.remote;

import java.util.List;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.network.ClientMapping;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * interface defining ClientMappingsRegistryProvider instances, used to install configured ClientMappingsRegistry services.
 *
 * @author Paul Ferraro
 */
public interface ClientMappingsRegistryProvider {

    Iterable<CapabilityServiceConfigurator> getServiceConfigurators(String connectorName, SupplierDependency<List<ClientMapping>> clientMappings);
}
