/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.undertow.security.digest;

import io.undertow.security.idm.Credential;

/**
 * An alternative {@link Credential} for Digest based authentication to pass over additional
 * information for authentication.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public interface DigestCredential extends Credential {

    /**
     * Get the digest received from the client.
     *
     * @return The digest received from the client.
     */
    String getClientDigest();

    /**
     * Get the nonce that was used for this request.
     *
     * @return The nonce that was used for this request.
     */
    String getNonce();

    /**
     * Get the nonce count for the request or {@code null} if none was used.
     *
     * @return The nonce count for the request or {@code null} if none was used.
     */
    String getNonceCount();

    /**
     * Get the client nonce for the request or {@code null} if none was used.
     *
     * @return The client nonce for the request or {@code null} if none was used.
     */
    String getClientNonce();

    /**
     * Get the QOP for the request or {@code null} if none was specified.
     *
     * @return The QOP for the request or {@code null} if none was specified.
     */
    String getQop();

    /**
     * Get the name of the realm used for this request.
     *
     * @return The name of the realm used for this request.
     */
    String getRealm();

    /**
     * Get the hashed representation of A2.
     *
     * @return The hashed representation of A2.
     */
    String getHA2();

}
