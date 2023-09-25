/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.deployment.jcedeployment.provider;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import java.nio.charset.StandardCharsets;
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
        return cipher.getBytes(StandardCharsets.UTF_8).clone();
    }

}
