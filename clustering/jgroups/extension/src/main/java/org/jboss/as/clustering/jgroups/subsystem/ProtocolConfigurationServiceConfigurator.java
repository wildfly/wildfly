/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
