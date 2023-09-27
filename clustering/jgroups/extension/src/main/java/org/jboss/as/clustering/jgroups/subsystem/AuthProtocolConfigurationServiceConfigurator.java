/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
