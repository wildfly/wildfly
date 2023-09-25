/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
