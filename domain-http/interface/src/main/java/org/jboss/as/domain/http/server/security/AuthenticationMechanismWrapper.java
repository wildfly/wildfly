/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.domain.http.server.security;

import io.undertow.security.api.AuthenticationMechanism;
import io.undertow.security.api.SecurityContext;
import io.undertow.server.HttpServerExchange;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * A wrapper around the {@link AuthenticationMechanism}s to ensure that the identity manager is aware of the current mechanism.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class AuthenticationMechanismWrapper implements AuthenticationMechanism {

    private final AuthenticationMechanism wrapped;
    private final org.jboss.as.domain.management.AuthMechanism mechanism;

    public AuthenticationMechanismWrapper(final AuthenticationMechanism wrapped, org.jboss.as.domain.management.AuthMechanism mechanism) {
        this.wrapped = wrapped;
        this.mechanism = mechanism;
    }

    @Override
    public AuthenticationMechanismOutcome authenticate(HttpServerExchange exchange, SecurityContext securityContext) {
        try {
            InetAddress inetAddress = null;
            SocketAddress address = exchange.getConnection().getPeerAddress();
            if (address instanceof InetSocketAddress) {
                inetAddress = ((InetSocketAddress)address).getAddress();
            }
            RealmIdentityManager.setRequestSpecific(mechanism, inetAddress);

            return wrapped.authenticate(exchange, securityContext);
        } finally {
            RealmIdentityManager.clearRequestSpecific();
        }
    }

    @Override
    public ChallengeResult sendChallenge(HttpServerExchange exchange, SecurityContext securityContext) {
        return wrapped.sendChallenge(exchange, securityContext);
    }

}
