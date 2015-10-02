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

public enum Authentication {
    RSA /* RSA auth */,
    DSS /* DSS auth */,
    aNULL /* no auth (i.e. use ADH or AECDH) */,
    DH /* Fixed DH auth (kDHd or kDHr) */,
    ECDH /* Fixed ECDH auth (kECDHe or kECDHr) */,
    KRB5 /* KRB5 auth */,
    ECDSA/* ECDSA auth*/,
    PSK /* PSK auth */,
    GOST94 /* GOST R 34.10-94 signature auth */,
    GOST01 /* GOST R 34.10-2001 */,
    FZA /* Fortezza */,
    SRP
}
