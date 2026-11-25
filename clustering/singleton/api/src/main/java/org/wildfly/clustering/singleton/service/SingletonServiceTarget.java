/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.service;

import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * A service target for singleton service installation.
 * @author Paul Ferraro
 */
public interface SingletonServiceTarget extends ServiceTarget {

    @Override
    SingletonServiceBuilder<?> addService();

    @Deprecated
    @Override
    SingletonServiceBuilder<?> addService(ServiceName name);

    @Deprecated
    @Override
    <T> SingletonServiceBuilder<T> addService(ServiceName name, org.jboss.msc.service.Service<T> service);

    @Override
    SingletonServiceTarget subTarget();
}
