/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.domain.http.server.Constants.FORBIDDEN;

import java.security.Principal;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;

import org.jboss.com.sun.net.httpserver.Authenticator;
import org.jboss.com.sun.net.httpserver.HttpExchange;
import org.jboss.com.sun.net.httpserver.HttpPrincipal;
import org.jboss.com.sun.net.httpserver.HttpsExchange;

/**
 * A simple authenticator to be used when the ONLY supported mechanism is ClientCert.
 *
 * Where alternative mechanisms are supported those mechanisms are responsible for checking if we already have a Principal.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ClientCertAuthenticator extends Authenticator {

    private final String realm;

    public ClientCertAuthenticator(final String realm) {
        this.realm = realm;
    }

    /**
     * @see org.jboss.com.sun.net.httpserver.Authenticator#authenticate(org.jboss.com.sun.net.httpserver.HttpExchange)
     */
    @Override
    public Result authenticate(HttpExchange exchange) {
        if (exchange instanceof HttpsExchange) {
            HttpsExchange httpsExch = (HttpsExchange) exchange;
            SSLSession session = httpsExch.getSSLSession();
            if (session != null) {
                try {
                    Principal p = session.getPeerPrincipal();

                    return new Success(new HttpPrincipal(p.getName(), realm));

                } catch (SSLPeerUnverifiedException e) {
                }
            }
        }

        return new Failure(FORBIDDEN);
    }

}
