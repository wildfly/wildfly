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

package org.wildfly.extension.undertow;

import java.util.function.Consumer;
import java.util.function.Supplier;

import io.undertow.security.impl.SingleSignOnManager;
import io.undertow.servlet.handlers.security.ServletSingleSignOnAuthenticationMechanism;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * @author Tomaz Cerar (c) 2014 Red Hat Inc.
 * @author Paul Ferraro
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class SingleSignOnService implements Service<SingleSignOnService> {
    public static final String AUTHENTICATION_MECHANISM_NAME = "SSO";
    private final String domain;
    private final String path;
    private final String cookieName;
    private final boolean httpOnly;
    private final boolean secure;
    private final Consumer<SingleSignOnService> serviceConsumer;
    private final Supplier<Host> host;
    private final Supplier<SingleSignOnManager> manager;

    SingleSignOnService(final Consumer<SingleSignOnService> serviceConsumer, final Supplier<Host> host,
                        final Supplier<SingleSignOnManager> manager, final String domain, final String path,
                        final boolean httpOnly, final boolean secure, final String cookieName) {
        this.serviceConsumer = serviceConsumer;
        this.host = host;
        this.manager = manager;
        this.domain = domain;
        this.path = path;
        this.httpOnly = httpOnly;
        this.secure = secure;
        this.cookieName = cookieName;
    }

    @Override
    public void start(final StartContext startContext) {
        ServletSingleSignOnAuthenticationMechanism mechanism = new ServletSingleSignOnAuthenticationMechanism(manager.get());
        mechanism.setDomain(this.domain);
        mechanism.setPath(this.path);
        mechanism.setHttpOnly(this.httpOnly);
        mechanism.setSecure(this.secure);
        mechanism.setCookieName(this.cookieName);
        host.get().registerAdditionalAuthenticationMechanism(AUTHENTICATION_MECHANISM_NAME, mechanism);
        serviceConsumer.accept(this);
    }

    @Override
    public void stop(final StopContext stopContext) {
        serviceConsumer.accept(null);
        host.get().unregisterAdditionalAuthenticationMechanism(AUTHENTICATION_MECHANISM_NAME);
    }

    @Override
    public SingleSignOnService getValue() {
        return this;
    }
}
