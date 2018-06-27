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

import java.io.DataInput;
import java.io.DataOutput;
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
    public void writeTo(DataOutput output) throws Exception {
        Util.writeByteBuffer(this.sharedSecret, output);
    }

    @Override
    public void readFrom(DataInput input) throws Exception {
        this.sharedSecret = Util.readByteBuffer(input);
    }
}
