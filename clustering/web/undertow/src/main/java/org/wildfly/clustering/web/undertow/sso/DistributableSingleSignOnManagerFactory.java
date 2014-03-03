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
package org.wildfly.clustering.web.undertow.sso;

import io.undertow.security.api.AuthenticatedSessionManager.AuthenticatedSession;
import io.undertow.server.session.SecureRandomSessionIdGenerator;

import org.wildfly.clustering.web.IdentifierFactory;
import org.wildfly.clustering.web.LocalContextFactory;
import org.wildfly.clustering.web.sso.SSOManager;
import org.wildfly.clustering.web.sso.SSOManagerFactory;
import org.wildfly.clustering.web.undertow.IdentifierFactoryAdapter;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.security.sso.SingleSignOnManager;
import org.wildfly.extension.undertow.security.sso.SingleSignOnManagerFactory;

/**
 * Factory for creating a distributable {@link SingleSignOnManagerFactory}.
 * @author Paul Ferraro
 */
public class DistributableSingleSignOnManagerFactory implements SingleSignOnManagerFactory, LocalContextFactory<Void> {

    private final SSOManagerFactory<AuthenticatedSession, String> factory;
    private final SessionManagerRegistry registry;

    public DistributableSingleSignOnManagerFactory(SSOManagerFactory<AuthenticatedSession, String> factory, SessionManagerRegistry registry) {
        this.factory = factory;
        this.registry = registry;
    }

    @Override
    public SingleSignOnManager createSingleSignOnManager(Host host) {
        IdentifierFactory<String> identifierFactory = new IdentifierFactoryAdapter(new SecureRandomSessionIdGenerator());
        SSOManager<AuthenticatedSession, String, Void> manager = this.factory.createSSOManager(identifierFactory, this);
        return new DistributableSingleSignOnManager(manager, this.registry);
    }

    @Override
    public Void createLocalContext() {
        return null;
    }
}
