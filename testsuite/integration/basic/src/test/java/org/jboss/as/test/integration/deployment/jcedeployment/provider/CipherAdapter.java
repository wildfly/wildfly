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
import javax.crypto.CipherSpi;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
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
public class CipherAdapter extends CipherSpi {

    DPCipher cipherInstance;

    protected CipherAdapter(String cipherName) {
        if (DummyProvider.DUMMY_CIPHER.equals(cipherName))
            cipherInstance = new DummyCipher();

        if (cipherInstance == null)
            throw new InternalError("Unsupported cipher function " + cipherName + "!");
    }

    @Override
    protected void engineSetMode(String mode) throws NoSuchAlgorithmException {
        cipherInstance.setMode(mode);
    }

    @Override
    protected void engineSetPadding(String padding) throws NoSuchPaddingException {
        cipherInstance.setPadding(padding);
    }

    @Override
    protected byte[] engineGetIV() {
        return cipherInstance.getIV();
    }

    @Override
    protected AlgorithmParameters engineGetParameters() {
        return cipherInstance.getParameters();
    }

    @Override
    protected int engineGetBlockSize() {
        return cipherInstance.getBlockSize();
    }

    @Override
    protected int engineGetOutputSize(int inputLen) {
        return cipherInstance.getOutputSize(inputLen);
    }

    @Override
    protected void engineInit(int opmode, Key key, SecureRandom secureRandom) throws InvalidKeyException {
        cipherInstance.init(opmode, key, secureRandom);
    }

    @Override
    protected void engineInit(int opmode, Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        cipherInstance.init(opmode, key, algorithmParameterSpec, secureRandom);
    }

    @Override
    protected void engineInit(int opmode, Key key, AlgorithmParameters algorithmParameters, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        cipherInstance.init(opmode, key, algorithmParameters, secureRandom);
    }

    @Override
    protected byte[] engineUpdate(byte[] input, int inputOffset, int inputLen) {
        return cipherInstance.update(input, inputOffset, inputLen);
    }

    @Override
    protected int engineUpdate(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws ShortBufferException {
        int result = engineGetOutputSize(inputLen);
        if ((output.length - outputOffset) < result)
            throw new ShortBufferException();

        byte[] buf = engineUpdate(input, inputOffset, inputLen);
        result = buf.length;
        if ((output.length - outputOffset) < result)
            throw new ShortBufferException();

        System.arraycopy(buf, 0, output, outputOffset, result);
        return result;
    }

    @Override
    protected byte[] engineDoFinal(byte[] input, int inputOffset, int inputLen) throws IllegalBlockSizeException, BadPaddingException {
        return cipherInstance.doFinal(input, inputOffset, inputLen);
    }

    @Override
    protected int engineDoFinal(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws ShortBufferException, IllegalBlockSizeException, BadPaddingException {
        int result = engineGetOutputSize(inputLen);
        if ((output.length - outputOffset) < result)
            throw new ShortBufferException();

        byte[] buf = engineDoFinal(input, inputOffset, inputLen);
        result = buf.length;
        if ((output.length - outputOffset) < result)
            throw new ShortBufferException();

        System.arraycopy(buf, 0, output, outputOffset, result);
        return result;
    }
}
