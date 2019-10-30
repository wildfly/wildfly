/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.ws.wsse.trust.shared;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * User: rsearls
 * Date: 2/5/14
 */
public class WSTrustAppUtils {

    public static String getServerHost() {
        final String host = System.getProperty("node0", "localhost");
        return toIPv6URLFormat(host);
    }

    private static String toIPv6URLFormat(final String host) {
        try {
            if (host.startsWith("[") || host.startsWith(":")) {
                if (System.getProperty("java.net.preferIPv4Stack") == null) {
                    throw new IllegalStateException("always provide java.net.preferIPv4Stack JVM property when using IPv6 address format");
                }
                if (System.getProperty("java.net.preferIPv6Addresses") == null) {
                    throw new IllegalStateException("always provide java.net.preferIPv6Addresses JVM property when using IPv6 address format");
                }
            }
            final boolean isIPv6Address = InetAddress.getByName(host) instanceof Inet6Address;
            final boolean isIPv6Formatted = isIPv6Address && host.startsWith("[");
            return isIPv6Address && !isIPv6Formatted ? "[" + host + "]" : host;
        } catch (final UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }
}
