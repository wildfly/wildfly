/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.security.KeyStore;

import javax.net.ssl.SSLContext;
import javax.sql.DataSource;

import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.network.SocketBinding;
import org.wildfly.clustering.service.UnaryRequirement;
import org.wildfly.security.credential.store.CredentialStore;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * Enumerates common unary requirements for clustering resources
 * @author Paul Ferraro
 */
public enum CommonUnaryRequirement implements UnaryRequirement, UnaryServiceNameFactoryProvider {
    CREDENTIAL_STORE("org.wildfly.security.credential-store", CredentialStore.class),
    DATA_SOURCE("org.wildfly.data-source", DataSource.class),
    KEY_STORE("org.wildfly.security.key-store", KeyStore.class),
    OUTBOUND_SOCKET_BINDING(OutboundSocketBinding.SERVICE_DESCRIPTOR),
    PATH(PathManager.PATH_SERVICE_DESCRIPTOR),
    SOCKET_BINDING(SocketBinding.SERVICE_DESCRIPTOR),
    SSL_CONTEXT("org.wildfly.security.ssl-context", SSLContext.class),
    ;
    private final String name;
    private final Class<?> type;
    private final UnaryServiceNameFactory factory = new UnaryRequirementServiceNameFactory(this);

    CommonUnaryRequirement(UnaryServiceDescriptor<?> descriptor) {
        this(descriptor.getName(), descriptor.getType());
    }

    CommonUnaryRequirement(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Class<?> getType() {
        return this.type;
    }

    @Override
    public UnaryServiceNameFactory getServiceNameFactory() {
        return this.factory;
    }
}
