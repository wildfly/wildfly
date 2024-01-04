/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.deployment.jcedeployment.provider;

import javax.crypto.BadPaddingException;
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
public interface DPCipher {
    String name();

    void setMode(String mode) throws NoSuchAlgorithmException;

    void setPadding(String padding) throws NoSuchPaddingException;

    byte[] getIV();

    AlgorithmParameters getParameters();

    int getBlockSize();

    int getOutputSize(int inputLen);

    void init(int opmode, Key key, SecureRandom secureRandom) throws InvalidKeyException;

    void init(int opmode, Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException;

    void init(int opmode, Key key, AlgorithmParameters algorithmParameters, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException;

    byte[] update(byte[] input, int inputOffset, int inputLen);

    byte[] doFinal(byte[] input, int inputOffset, int inputLen) throws IllegalBlockSizeException, BadPaddingException;
}
