/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
