/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.service.SimpleServiceConfigurator;

/**
 * A {@link CapabilityServiceConfigurator} that provides a static value.
 * @author Paul Ferraro
 */
public class SimpleCapabilityServiceConfigurator<T> extends SimpleServiceConfigurator<T> implements CapabilityServiceConfigurator {

    /**
     * Constructs a new {@link CapabilityServiceConfigurator}.
     * @param name the service name
     * @param value the static value provided by the service
     */
    public SimpleCapabilityServiceConfigurator(ServiceName name, T value) {
        super(name, value);
    }
}
