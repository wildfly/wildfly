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
package org.wildfly.test.security.common.other;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.ZonedDateTime;

import javax.security.auth.x500.X500Principal;

import org.wildfly.security.x500.cert.X509CertificateBuilder;

/**
 * Common methods for key-pair and X509 certificates generation.
 *
 * @author Jan Stourac
 */
public final class KeyUtils {
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    // One year in seconds
    private static final long DEFAULT_CERT_VALIDITY = 365L * 24L * 60L * 60L;

    private static final String DEFAULT_KEY_ALGORITHM = "RSA";
    private static final int DEFAULT_KEY_SIZE = 2048;

    /**
     * Generates key-pair with {@link KeyUtils#DEFAULT_KEY_ALGORITHM} algorithm and {@link KeyUtils#DEFAULT_KEY_SIZE}
     * key size.
     *
     * @return generated key-pair
     * @throws NoSuchAlgorithmException
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        return generateKeyPair(DEFAULT_KEY_ALGORITHM, DEFAULT_KEY_SIZE);
    }

    /**
     * Generates key-pair with given algorithm and key size.
     *
     * @param algorithm
     * @param keySize
     * @return generated key-pair
     * @throws NoSuchAlgorithmException
     */
    public static KeyPair generateKeyPair(String algorithm, int keySize) throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(algorithm);
        keyPairGenerator.initialize(keySize);
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * Generates a self-signed certificate using {@link KeyUtils#SIGNATURE_ALGORITHM}. The certificate
     * will use a distinguished name of the form {@code CN=name} and will be valid for 1 year.
     *
     * @param name    common name for the certificate
     * @param keyPair public and private keys
     * @return generated certificate
     * @throws CertificateException
     */
    public static X509Certificate generateX509Certificate(String name, KeyPair keyPair) throws CertificateException {
        return generateX509Certificate(name, keyPair, DEFAULT_CERT_VALIDITY, SIGNATURE_ALGORITHM);
    }

    /**
     * Generates self-signed certificate for provided key-pair with given validity time and signature algorithm.
     *
     * @param name               common name for the certificate
     * @param keyPair            public and private keys
     * @param certValidity       how long the certificate should be valid to the future (number of seconds)
     * @param signatureAlgorithm signature algorithm
     * @return generated certificate
     * @throws CertificateException
     */
    public static X509Certificate generateX509Certificate(String name, KeyPair keyPair, long certValidity, String
            signatureAlgorithm) throws CertificateException {
        ZonedDateTime from = ZonedDateTime.now();
        ZonedDateTime to = ZonedDateTime.now().plusSeconds(certValidity);
        BigInteger serialNumber = new BigInteger(64, new SecureRandom());
        X500Principal owner = new X500Principal("CN=" + name);

        X509CertificateBuilder certificateBuilder = new X509CertificateBuilder();
        return certificateBuilder.setIssuerDn(owner).setSubjectDn(owner).setNotValidBefore(from).setNotValidAfter(to)
                .setSerialNumber(serialNumber).setPublicKey(keyPair.getPublic()).setSignatureAlgorithmName
                        (signatureAlgorithm).setSigningKey(keyPair.getPrivate()).build();
    }
}
