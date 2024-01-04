/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.mail.extension;

import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.mail.Session;

import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service that provides a jakarta.mail.Session.
 *
 * @author Tomaz Cerar
 * @created 27.7.11 0:14
 */
public class MailSessionService implements Service {
    private final Consumer<Session> session;
    private final Supplier<SessionProvider> provider;

    public MailSessionService(Consumer<Session> session, Supplier<SessionProvider> provider) {
        this.session = session;
        this.provider = provider;
    }

    @Override
    public void start(final StartContext startContext) throws StartException {
        this.session.accept(this.provider.get().getSession());
    }

    @Override
    public void stop(final StopContext stopContext) {
        // Nothing to stop
    }
}
