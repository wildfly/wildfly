/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.clustering.web.cache.session;

import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.wildfly.clustering.web.session.ImmutableSession;
import org.wildfly.clustering.web.session.SessionManagerConfiguration;

/**
 * A {@link SessionManagerConfiguration} implementation that delegates to another {@link SessionManagerConfiguration}.
 * @author Paul Ferraro
 * @param <SC> the servlet context type
 */
public class DelegatingSessionManagerConfiguration<SC> implements SessionManagerConfiguration<SC> {

    private final SessionManagerConfiguration<SC> configuration;

    public DelegatingSessionManagerConfiguration(SessionManagerConfiguration<SC> configuration) {
        this.configuration = configuration;
    }

    @Override
    public Consumer<ImmutableSession> getExpirationListener() {
        return this.configuration.getExpirationListener();
    }

    @Override
    public Duration getTimeout() {
        return this.configuration.getTimeout();
    }

    @Override
    public SC getServletContext() {
        return this.configuration.getServletContext();
    }

    @Override
    public Supplier<String> getIdentifierFactory() {
        return this.configuration.getIdentifierFactory();
    }
}
