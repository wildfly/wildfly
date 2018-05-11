/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
import org.jboss.msc.service.ServiceBuilder;
import org.jgroups.auth.AuthToken;
import org.jgroups.protocols.AUTH;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Paul Ferraro
 */
public class AuthProtocolConfigurationServiceConfigurator extends ProtocolConfigurationServiceConfigurator<AUTH> {

    private final SupplierDependency<AuthToken> token;

    public AuthProtocolConfigurationServiceConfigurator(PathAddress address) {
        super(address);
        this.token = new ServiceSupplierDependency<>(AuthTokenResourceDefinition.Capability.AUTH_TOKEN.getServiceName(address.append(AuthTokenResourceDefinition.WILDCARD_PATH)));
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        return super.register(this.token.register(builder));
    }

    @Override
    public void accept(AUTH protocol) {
        protocol.setAuthToken(this.token.get());
    }
}
