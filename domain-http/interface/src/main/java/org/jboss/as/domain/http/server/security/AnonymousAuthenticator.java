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

package org.jboss.as.domain.http.server.security;

import java.security.Principal;
import java.util.Set;

import javax.security.auth.Subject;

import org.jboss.as.domain.management.security.RealmUser;
import org.jboss.com.sun.net.httpserver.Authenticator;
import org.jboss.com.sun.net.httpserver.HttpExchange;
import org.jboss.com.sun.net.httpserver.HttpExchange.AttributeScope;
import org.jboss.com.sun.net.httpserver.HttpPrincipal;

/**
 * Simple authenticator to set current user to anonymous@anonymous for all requests.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class AnonymousAuthenticator extends Authenticator {

    private static final String ANONYMOUS = "anonymous";

    /**
     * Implementation of authenticate that always authenticates as anonymous@anonymous
     *
     * @see org.jboss.com.sun.net.httpserver.Authenticator#authenticate(org.jboss.com.sun.net.httpserver.HttpExchange)
     */
    @Override
    public Result authenticate(final HttpExchange exchange) {
        Subject subject = (Subject) exchange.getAttribute(Subject.class.getName(), AttributeScope.CONNECTION);

        if (subject != null) {
            Set<HttpPrincipal> httpPrincipals = subject.getPrincipals(HttpPrincipal.class);
            if (httpPrincipals.size() > 0) {
                return new Success(httpPrincipals.iterator().next());
            }
        }

        HttpPrincipal principal = new HttpPrincipal(ANONYMOUS, ANONYMOUS);
        RealmUser realmUser = new RealmUser(ANONYMOUS);
        subject = new Subject();
        Set<Principal> principals = subject.getPrincipals();
        principals.add(principal);
        principals.add(realmUser);
        exchange.setAttribute(Subject.class.getName(), subject, AttributeScope.CONNECTION);

        return new Authenticator.Success(principal);
    }

}
