/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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

package org.jboss.as.mail.extension;

import java.util.function.Consumer;

import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service that provides a {@link SessionProvider} and its {@link MailSessionConfig} (for use by tests).
 * @author Paul Ferraro
 */
class ConfigurableSessionProviderService implements Service {
    private final Consumer<ConfigurableSessionProvider> provider;
    private final MailSessionConfig config;

    ConfigurableSessionProviderService(Consumer<ConfigurableSessionProvider> provider, MailSessionConfig config) {
        this.provider = provider;
        this.config = config;
    }

    @Override
    public void start(final StartContext startContext) throws StartException {
        this.provider.accept(SessionProviderFactory.create(this.config));
    }

    @Override
    public void stop(final StopContext stopContext) {
        // Nothing to stop
    }
}
