/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller;

import java.security.KeyStore;

import javax.net.ssl.SSLContext;
import javax.sql.DataSource;

import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.network.SocketBinding;
import org.wildfly.clustering.service.UnaryRequirement;
import org.wildfly.security.credential.store.CredentialStore;

/**
 * Enumerates common unary requirements for clustering resources
 * @author Paul Ferraro
 */
public enum CommonUnaryRequirement implements UnaryRequirement, UnaryServiceNameFactoryProvider {
    CREDENTIAL_STORE("org.wildfly.security.credential-store", CredentialStore.class),
    DATA_SOURCE("org.wildfly.data-source", DataSource.class),
    KEY_STORE("org.wildfly.security.key-store", KeyStore.class),
    OUTBOUND_SOCKET_BINDING("org.wildfly.network.outbound-socket-binding", OutboundSocketBinding.class),
    PATH("org.wildfly.management.path", String.class),
    SOCKET_BINDING("org.wildfly.network.socket-binding", SocketBinding.class),
    SSL_CONTEXT("org.wildfly.security.ssl-context", SSLContext.class),
    ;
    private final String name;
    private final Class<?> type;
    private final UnaryServiceNameFactory factory = new UnaryRequirementServiceNameFactory(this);

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
