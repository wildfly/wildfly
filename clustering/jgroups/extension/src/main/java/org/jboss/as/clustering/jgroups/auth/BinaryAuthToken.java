/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.auth;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import org.jgroups.Message;
import org.jgroups.auth.AuthToken;
import org.jgroups.util.Util;

/**
 * An AUTH token, analogous to {@link org.jgroups.auth.SimpleToken}, but uses a binary shared secret, instead of a case-insensitive string comparison.
 * @author Paul Ferraro
 */
public class BinaryAuthToken extends AuthToken {

    private volatile byte[] sharedSecret;

    public BinaryAuthToken() {
        this.sharedSecret = null;
    }

    public BinaryAuthToken(byte[] sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    public byte[] getSharedSecret() {
        return this.sharedSecret;
    }

    @Override
    public boolean authenticate(AuthToken token, Message message) {
        if ((this.sharedSecret == null) || !(token instanceof BinaryAuthToken)) return false;
        return Arrays.equals(this.sharedSecret, ((BinaryAuthToken) token).sharedSecret);
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public int size() {
        return Util.size(this.sharedSecret);
    }

    @Override
    public void writeTo(DataOutput output) throws IOException {
        Util.writeByteBuffer(this.sharedSecret, output);
    }

    @Override
    public void readFrom(DataInput input) throws IOException {
        this.sharedSecret = Util.readByteBuffer(input);
    }
}
