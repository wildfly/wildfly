/*
 * Copyright (C) 2015 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */

package org.wildfly.extension.undertow.security.openssl;

import static org.wildfly.extension.undertow.security.openssl.Constants.SSL_PROTO_SSLv2;
import static org.wildfly.extension.undertow.security.openssl.Constants.SSL_PROTO_SSLv3;
import static org.wildfly.extension.undertow.security.openssl.Constants.SSL_PROTO_TLSv1_2;


enum Protocol {
    SSLv3(SSL_PROTO_SSLv3),
    SSLv2(SSL_PROTO_SSLv2),
    TLSv1(SSL_PROTO_SSLv3),
    TLSv1_2(SSL_PROTO_TLSv1_2);

    private final String openSSLName;

    private Protocol(String openSSLName) {
        this.openSSLName = openSSLName;
    }

    /**
     * The name returned by OpenSSL in the protocol column when using
     * <code>openssl ciphers -v</code>. This is currently only used by the unit
     * tests hence it is package private.
     */
    String getOpenSSLName() {
        return openSSLName;
    }
}
