/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.clustering.controller.ResourceServiceNameFactory;
import org.jboss.as.controller.PathAddress;
import org.jboss.msc.service.ServiceController;

/**
 * Configures a service supplying a global component.
 * @author Paul Ferraro
 */
public abstract class GlobalComponentServiceConfigurator<C> extends ComponentServiceConfigurator<C> {

    GlobalComponentServiceConfigurator(ResourceServiceNameFactory factory, PathAddress address) {
        this(factory, address, ServiceController.Mode.PASSIVE);
    }

    GlobalComponentServiceConfigurator(ResourceServiceNameFactory factory, PathAddress address, ServiceController.Mode initialMode) {
        super(factory, address, initialMode);
    }
}
