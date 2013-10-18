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

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.Principal;

import javax.security.auth.Subject;

import org.jboss.as.controller.security.InetAddressPrincipal;
import org.jboss.as.core.security.RealmUser;

/**
 * An {@link AuthenticationMechanism} that always associates an 'anonymous' user.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class AnonymousMechanism implements AuthenticationMechanism {

    private static final String ANONYMOUS_USER = "anonymous";
    private static final String ANONYMOUS_MECH = "ANONYMOUS";

    @Override
    public AuthenticationMechanismOutcome authenticate(HttpServerExchange exchange, SecurityContext context) {
        Principal user = new RealmUser(ANONYMOUS_USER);
        Subject subject = new Subject();
        subject.getPrincipals().add(user);
        SocketAddress address = exchange.getConnection().getPeerAddress();
        if (address instanceof InetSocketAddress) {
            subject.getPrincipals().add(new InetAddressPrincipal(((InetSocketAddress) address).getAddress()));
        }

        context.authenticationComplete(new RealmIdentityAccount(subject, user), ANONYMOUS_MECH);

        return AuthenticationMechanismOutcome.AUTHENTICATED;
    }

    @Override
    public ChallengeResult sendChallenge(HttpServerExchange arg0, SecurityContext arg1) {
        // Anonymous will always succeed so no challenge to send.
        return new ChallengeResult(false);
    }

}
