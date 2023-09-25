/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.TransportConfiguration;
import org.jboss.as.controller.PathAddress;
import org.jboss.msc.service.ServiceController;

/**
 * @author Paul Ferraro
 */
public class NoTransportServiceConfigurator extends GlobalComponentServiceConfigurator<TransportConfiguration> {

    NoTransportServiceConfigurator(PathAddress address) {
        super(CacheContainerComponent.TRANSPORT, address, ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public TransportConfiguration get() {
        return new GlobalConfigurationBuilder().transport().transport(null).create();
    }
}
