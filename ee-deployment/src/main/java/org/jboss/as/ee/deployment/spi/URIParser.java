/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.ee.deployment.spi;

import java.net.URI;

/**
 * Utility class for URI parsing.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 02-Aug-2011
 */
final class URIParser {

    private URI uri;

    URIParser(URI uri) {
        this.uri = uri;
        if (uri == null)
            throw new IllegalArgumentException("Null uri");
    }

    String getParameter(String key) {
        String value = null;

        String query = uri.getQuery();
        String[] params = {};
        if (query != null) {
            params = query.split("&|=");
        }
        for (int n = 0; n < params.length; n += 2) {
            if (key.equals(params[n])) {
                value = params[n + 1];
            }
        }

        return value;
    }
}
