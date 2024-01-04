/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service;

import org.jboss.msc.service.ServiceName;

/**
 * {@link ServiceNameProvider} using a pre-defined {@link ServiceName}
 * @author Paul Ferraro
 */
public class SimpleServiceNameProvider implements ServiceNameProvider {

    private final ServiceName name;

    public SimpleServiceNameProvider(ServiceName name) {
        this.name = name;
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (!(object instanceof ServiceNameProvider)) return false;
        ServiceNameProvider provider = (ServiceNameProvider) object;
        return this.name.equals(provider.getServiceName());
    }

    @Override
    public String toString() {
        return this.name.getCanonicalName();
    }
}
