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

enum KeyExchange {
    EECDH /* SSL_kECDHE - ephemeral ECDH */,
    RSA   /* SSL_kRSA   - RSA key exchange */,
    DHr   /* SSL_kDHr   - DH cert, RSA CA cert */ /* no such ciphersuites supported! */,
    DHd   /* SSL_kDHd   - DH cert, DSA CA cert */ /* no such ciphersuite supported! */,
    EDH   /* SSL_kDHE   - tmp DH key no DH cert */,
    PSK   /* SSK_kPSK   - PSK */,
    FZA   /* SSL_kFZA   - Fortezza */  /* no such ciphersuite supported! */,
    KRB5  /* SSL_kKRB5  - Kerberos 5 key exchange */,
    ECDHr /* SSL_kECDHr - ECDH cert, RSA CA cert */,
    ECDHe /* SSL_eECDHe - ECDH cert, ECDSA CA cert */,
    GOST  /* SSL_kGOST  - GOST key exchange */,
    SRP   /* SSL_kSRP   - SRP */,
    RSAPSK,
    ECDHEPSK,
    DHEPSK;
}
