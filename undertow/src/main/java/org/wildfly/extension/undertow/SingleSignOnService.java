/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2014, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.extension.undertow;

import io.undertow.security.impl.SingleSignOnManager;
import io.undertow.servlet.handlers.security.ServletSingleSignOnAuthenticationMechainism;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Tomaz Cerar (c) 2014 Red Hat Inc.
 * @author Paul Ferraro
 */
class SingleSignOnService implements Service<SingleSignOnService> {

    private static final String AUTHENTICATION_MECHANISM_NAME = "SSO";

    private final String domain;
    private final String path;
    private final boolean httpOnly;
    private final boolean secure;
    private final InjectedValue<Host> host = new InjectedValue<>();
    private final InjectedValue<SingleSignOnManager> manager = new InjectedValue<>();

    SingleSignOnService(String domain, String path, boolean httpOnly, boolean secure, String cookieName) {
        this.domain = domain;
        this.path = path;
        this.httpOnly = httpOnly;
        this.secure = secure;
    }

    @Override
    public void start(StartContext startContext) {
        Host host = this.host.getValue();
        ServletSingleSignOnAuthenticationMechainism mechanism = new ServletSingleSignOnAuthenticationMechainism(this.manager.getValue());
        mechanism.setDomain(this.domain);
        mechanism.setPath(this.path);
        mechanism.setHttpOnly(httpOnly);
        mechanism.setSecure(secure);
        host.registerAdditionalAuthenticationMechanism(AUTHENTICATION_MECHANISM_NAME, mechanism);
    }

    @Override
    public void stop(StopContext stopContext) {
        Host host = this.host.getValue();
        host.unregisterAdditionalAuthenticationMechanism(AUTHENTICATION_MECHANISM_NAME);
    }

    @Override
    public SingleSignOnService getValue() {
        return this;
    }

    Injector<Host> getHost() {
        return this.host;
    }

    Injector<SingleSignOnManager> getSingleSignOnSessionManager() {
        return this.manager;
    }
}
