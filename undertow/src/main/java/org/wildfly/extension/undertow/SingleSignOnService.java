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

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.undertow.security.sso.SingleSignOnAuthenticationMechanism;

/**
 * @author Tomaz Cerar (c) 2014 Red Hat Inc.
 * @author Paul Ferraro
 */
public class SingleSignOnService implements Service<SingleSignOnService> {

    private static final String AUTHENTICATION_MECHANISM_NAME = "SSO";

    private final String domain;
    private final InjectedValue<Host> host = new InjectedValue<>();
    private final InjectedValue<SingleSignOnManager> manager = new InjectedValue<>();

    public SingleSignOnService(String domain) {
        this.domain = domain;
    }

    @Override
    public void start(StartContext startContext) {
        SingleSignOnAuthenticationMechanism mechanism = new SingleSignOnAuthenticationMechanism(this.manager.getValue());
        if (this.domain != null) {
            mechanism.setDomain(this.domain);
        }

        this.host.getValue().registerAdditionalAuthenticationMechanism(AUTHENTICATION_MECHANISM_NAME, mechanism);
    }

    @Override
    public void stop(StopContext stopContext) {
        this.host.getValue().unregisterAdditionalAuthenticationMechanism(AUTHENTICATION_MECHANISM_NAME);
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
