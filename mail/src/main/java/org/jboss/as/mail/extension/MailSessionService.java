/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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


import java.util.HashMap;
import java.util.Map;
import javax.mail.Session;

import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.inject.MapInjector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service that provides a javax.mail.Session.
 *
 * @author Tomaz Cerar
 * @created 27.7.11 0:14
 */
public class MailSessionService implements Service<Session> {
    private final MailSessionConfig config;
    private volatile SessionProvider provider;
    private Map<String, OutboundSocketBinding> socketBindings = new HashMap<String, OutboundSocketBinding>();

    public MailSessionService(MailSessionConfig config) {
        MailLogger.ROOT_LOGGER.tracef("service constructed with config: %s", config);
        this.config = config;
    }

    public MailSessionConfig getConfig() {
        return config;
    }

    public void start(StartContext startContext) throws StartException {
        MailLogger.ROOT_LOGGER.trace("start...");
        provider = SessionProviderFactory.create(config, socketBindings);
    }

    public void stop(StopContext stopContext) {
        MailLogger.ROOT_LOGGER.trace("stop...");
    }

    Injector<OutboundSocketBinding> getSocketBindingInjector(String name) {
        return new MapInjector<>(socketBindings, name);
    }

    public Session getValue() throws IllegalStateException, IllegalArgumentException {
        return provider.getSession();
    }
}
