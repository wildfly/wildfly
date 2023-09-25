/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
