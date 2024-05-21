/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.ServiceNameFactory;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheProvider;
import org.jboss.msc.service.ServiceName;

/**
 * @author Paul Ferraro
 */
@Deprecated
public class LegacyCacheFactoryRemove extends ServiceRemoveStepHandler {

    LegacyCacheFactoryRemove(LegacyCacheFactoryAdd addHandler) {
        super(addHandler);
    }

    @Override
    protected ServiceName serviceName(final String name) {
        return ServiceNameFactory.resolveServiceName(StatefulSessionBeanCacheProvider.SERVICE_DESCRIPTOR, name);
    }
}
