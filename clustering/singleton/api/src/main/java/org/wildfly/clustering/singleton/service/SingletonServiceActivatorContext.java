/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.service;

import org.jboss.msc.service.ServiceActivatorContext;

/**
 * A {@link ServiceActivatorContext} whose service target instruments singleton service installation.
 * @author Paul Ferraro
 */
public interface SingletonServiceActivatorContext extends ServiceActivatorContext {

    @Override
    SingletonServiceTarget getServiceTarget();
}
