/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.controller.PathAddress;
import org.jboss.msc.service.ServiceName;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.service.ServiceNameProvider;

/**
 * @author Paul Ferraro
 */
public class ProtocolConfigurationServiceConfigurator<P extends Protocol> extends AbstractProtocolConfigurationServiceConfigurator<P, ProtocolConfiguration<P>> {

    private final ServiceNameProvider provider;

    public ProtocolConfigurationServiceConfigurator(PathAddress address) {
        super(address.getLastElement().getValue());
        this.provider = new ProtocolServiceNameProvider(address);
    }

    @Override
    public ServiceName getServiceName() {
        return this.provider.getServiceName();
    }

    @Override
    public ProtocolConfiguration<P> get() {
        return this;
    }

    @Override
    public void accept(P protocol) {
    }
}
