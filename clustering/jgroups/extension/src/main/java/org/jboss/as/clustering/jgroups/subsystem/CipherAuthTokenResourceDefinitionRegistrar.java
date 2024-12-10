/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.crypto.Cipher;

import org.jboss.as.clustering.jgroups.auth.CipherAuthToken;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for a cipher authentication token for use by the AUTH protocol.
 * @author Paul Ferraro
 */
public class CipherAuthTokenResourceDefinitionRegistrar extends AuthTokenResourceDefinitionRegistrar<CipherAuthToken> {

    CipherAuthTokenResourceDefinitionRegistrar() {
        super(CipherAuthTokenResourceDescription.INSTANCE);
    }

    @Override
    public ServiceDependency<Function<byte[], CipherAuthToken>> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        String keyAlias = CipherAuthTokenResourceDescription.Attribute.KEY_ALIAS.resolveModelAttribute(context, model).asString();
        String transformation = CipherAuthTokenResourceDescription.Attribute.ALGORITHM.resolveModelAttribute(context, model).asString();

        return CipherAuthTokenResourceDescription.KEY_STORE.resolve(context, model).combine(CipherAuthTokenResourceDescription.KEY_CREDENTIAL.resolve(context, model).map(CLEAR_PASSWORD_CREDENTIAL), new BiFunction<>() {
            @Override
            public Function<byte[], CipherAuthToken> apply(KeyStore store, char[] password) {
                return new Function<>() {
                    @Override
                    public CipherAuthToken apply(byte[] secret) {
                        try {
                            if (!store.containsAlias(keyAlias)) {
                                throw JGroupsLogger.ROOT_LOGGER.keyEntryNotFound(keyAlias);
                            }
                            if (!store.entryInstanceOf(keyAlias, KeyStore.PrivateKeyEntry.class)) {
                                throw JGroupsLogger.ROOT_LOGGER.unexpectedKeyStoreEntryType(keyAlias, KeyStore.PrivateKeyEntry.class.getSimpleName());
                            }
                            KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) store.getEntry(keyAlias, new KeyStore.PasswordProtection(password));
                            KeyPair pair = new KeyPair(entry.getCertificate().getPublicKey(), entry.getPrivateKey());
                            Cipher cipher = Cipher.getInstance(transformation);
                            return new CipherAuthToken(cipher, pair, secret);
                        } catch (GeneralSecurityException e) {
                            throw new IllegalArgumentException(e);
                        }
                    }
                };
            }
        });
    }
}
