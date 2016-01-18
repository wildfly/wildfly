/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
