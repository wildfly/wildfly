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
package org.wildfly.extension.undertow.security.sso;

import java.util.Map;

import org.wildfly.extension.undertow.Host;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.AuthenticationMechanismFactory;
import io.undertow.security.impl.SingleSignOnAuthenticationMechanism;
import io.undertow.server.handlers.form.FormParserFactory;

/**
 * Factory for creating a single sign on {@link AuthenticationMechanism}.
 * @author Paul Ferraro
 */
public class SingleSignOnAuthenticationMechanismFactory implements AuthenticationMechanismFactory {

    private final SingleSignOnManagerFactory factory;
    private final Host host;

    public SingleSignOnAuthenticationMechanismFactory(SingleSignOnManagerFactory factory, Host host) {
        this.factory = factory;
        this.host = host;
    }

    @Override
    public AuthenticationMechanism create(String mechanismName, FormParserFactory formParserFactory, Map<String, String> properties) {
        return new SingleSignOnAuthenticationMechanism(this.factory.createSingleSignOnManager(this.host));
    }
}
