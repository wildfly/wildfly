/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import org.jboss.sasl.util.HexConverter;

/**
 * A simple NonceFactory for single use nonces.
 * <p/>
 * At a later point a pluggable mechansims may be added to control the nonce requirements and add additional generation
 * strategies. Issues such as expiration will also be handled.
 * <p/>
 * This implementation uses a SecureRandom to generate 16 random bytes which will then be converted to hex as 32 characters.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class NonceFactory {

    private static final String MAX_CACHED_NONCES_PROPERTY = "org.jboss.domain.http.MAX_CACHED_NONCES";
    private static final int DEFAULT_MAX_CACHED_NONCES = 50;

    private static final int MAX_CACHED_NONCES = SecurityActions.getInt(MAX_CACHED_NONCES_PROPERTY, DEFAULT_MAX_CACHED_NONCES);

    private static final SecureRandom srand = new SecureRandom();

    private static final int NONCE_BYTES = 16;

    private final List<String> issuedNonces = new LinkedList<String>();

    /**
     * Generates a nonce and caches it with the issued nonces for later verification.
     *
     * @return a newly generated nonce.
     */
    public String createNonce(boolean store) {
        byte[] newNonce = new byte[NONCE_BYTES];
        Random random = new Random(srand.nextLong());
        random.nextBytes(newNonce);
        String nonceString = HexConverter.convertToHexString(newNonce);
        if (store) {
            synchronized (issuedNonces) {
                issuedNonces.add(nonceString);
                while (issuedNonces.size() > MAX_CACHED_NONCES) {
                    issuedNonces.remove(0);
                }
            }
        }

        return nonceString;
    }

    /**
     * Validates the nonce is in the list of issued nonces and removes it from the list to ensure it can not be used again.
     *
     * @param nonceToUse - The nonce to validate.
     * @return true if this nonce was found in the issues nonces cache, false if this nonce is not valid.
     */
    public boolean useNonce(String nonceToUse) {

        synchronized (issuedNonces) {
            return issuedNonces.remove(nonceToUse);
        }

    }

}
