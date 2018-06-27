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

package org.jboss.as.clustering.jgroups.auth;

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;

import org.jgroups.Message;
import org.jgroups.auth.AuthToken;

/**
 * An AUTH token, functionally equivalent to {@link org.jgroups.auth.X509Token}, but configured using Elytron resources.
 * @author Paul Ferraro
 */
public class CipherAuthToken extends BinaryAuthToken {

    private final Cipher cipher;
    private final byte[] rawSharedSecret;

    public CipherAuthToken() {
        super();
        this.cipher = null;
        this.rawSharedSecret = null;
    }

    public CipherAuthToken(Cipher cipher, KeyPair pair, byte[] rawSharedSecret) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        super(encryptedSharedSecret(cipher, pair.getPublic(), rawSharedSecret));
        this.cipher = cipher;
        this.rawSharedSecret = rawSharedSecret;
        this.cipher.init(Cipher.DECRYPT_MODE, pair.getPrivate());
    }

    private static byte[] encryptedSharedSecret(Cipher cipher, Key key, byte[] data) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    @Override
    public synchronized boolean authenticate(AuthToken token, Message message) {
        if ((this.getSharedSecret() == null) || !(token instanceof CipherAuthToken)) return false;
        try {
            byte[] decrypted = this.cipher.doFinal(((CipherAuthToken) token).getSharedSecret());
            return Arrays.equals(decrypted, this.rawSharedSecret);
        } catch (GeneralSecurityException e) {
            return false;
        }
    }
}
