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

import io.undertow.security.impl.InMemorySingleSignOnManager;
import io.undertow.security.impl.SingleSignOnAuthenticationMechanism;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Tomaz Cerar (c) 2014 Red Hat Inc.
 */
public class SingleSignOnService implements Service<SingleSignOnService> {

    private final String domain;
    private final boolean reAuthenticate;
    private final InjectedValue<Host> host = new InjectedValue<>();


    public SingleSignOnService(String domain, boolean reAuthenticate) {
        this.domain = domain;
        this.reAuthenticate = reAuthenticate;
    }

    @Override
    public void start(StartContext startContext) throws StartException {
        SingleSignOnAuthenticationMechanism authenticationMechanism = createInMemory();

        host.getValue().registerAdditionalAuthenticationMechanism("sso", authenticationMechanism);
    }

    private SingleSignOnAuthenticationMechanism createInMemory() {
        return new SingleSignOnAuthenticationMechanism(new InMemorySingleSignOnManager()).setDomain(domain);
    }

    @Override
    public void stop(StopContext stopContext) {

    }

    InjectedValue<Host> getHost() {
        return host;
    }

    @Override
    public SingleSignOnService getValue() throws IllegalStateException {
        return this;
    }


}
