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

import java.net.InetAddress;
import java.security.Principal;
import java.util.Collection;

import org.jboss.as.controller.security.InetAddressPrincipal;
import org.jboss.com.sun.net.httpserver.HttpExchange;

/**
 * A utility used for adding an {@link InetAddressPrincipal} to a {@link Principal} {@link Collection}.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class PrincipalUtil {

    static void addInetPrincipal(final HttpExchange exchange, final Collection<Principal> principals) {
        InetAddress address = exchange.getRemoteAddress().getAddress();
        if (address != null) {
            principals.add(new InetAddressPrincipal(address));
        }
    }
}
