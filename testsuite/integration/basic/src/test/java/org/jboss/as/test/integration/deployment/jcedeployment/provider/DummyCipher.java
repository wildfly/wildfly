/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.deployment.jcedeployment.provider;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

/**
 * @author <a href="mailto:istudens@redhat.com">Ivo Studensky</a>
 */
public class DummyCipher implements DPCipher {

    String cipher;
    int opmode;

    public DummyCipher() {
    }

    public String name() {
        return DummyProvider.DUMMY_CIPHER;
    }

    @Override
    public void setMode(String mode) throws NoSuchAlgorithmException {
        // nothing to do
    }

    @Override
    public void setPadding(String padding) throws NoSuchPaddingException {
        // nothing to do
    }

    @Override
    public void init(int opmode, Key key, SecureRandom secureRandom) throws InvalidKeyException {
        init(opmode);
    }

    @Override
    public void init(int opmode, Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        init(opmode);
    }

    @Override
    public void init(int opmode, Key key, AlgorithmParameters algorithmParameters, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        init(opmode);
    }

    private void init(int opmode) {
        this.opmode = opmode;
        switch (opmode) {
            case Cipher.ENCRYPT_MODE:
                cipher = "decrypted";
                break;
            case Cipher.DECRYPT_MODE:
                cipher = "encrypted";
                break;
        }
    }

    @Override
    public byte[] getIV() {
        return null;
    }

    @Override
    public AlgorithmParameters getParameters() {
        return null;
    }

    @Override
    public int getBlockSize() {
        return 0;
    }

    @Override
    public int getOutputSize(int inputLen) {
        return "encrypted".length();
    }

    @Override
    public byte[] update(byte[] input, int inputOffset, int inputLen) {
        return new byte[0];
    }

    @Override
    public byte[] doFinal(byte[] input, int inputOffset, int inputLen) throws IllegalBlockSizeException, BadPaddingException {
        switch (opmode) {
            case Cipher.ENCRYPT_MODE:
                cipher = "encrypted";
                break;
            case Cipher.DECRYPT_MODE:
                cipher = "decrypted";
                break;
        }
        return cipher.getBytes().clone();
    }

}
