/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.service;

import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.ServiceConfigurator;

/**
 * An immutable {@link ServiceConfigurator} used to build a singleton service.
 * @author Paul Ferraro
 */
public interface ImmutableSingletonServiceConfigurator extends ServiceConfigurator {

    @Override
    SingletonServiceBuilder<?> build(ServiceTarget target);
}
