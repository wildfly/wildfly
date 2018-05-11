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

package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.clustering.jgroups.subsystem.CipherAuthTokenResourceDefinition.Attribute.ALGORITHM;
import static org.jboss.as.clustering.jgroups.subsystem.CipherAuthTokenResourceDefinition.Attribute.KEY_ALIAS;
import static org.jboss.as.clustering.jgroups.subsystem.CipherAuthTokenResourceDefinition.Attribute.KEY_CREDENTIAL;
import static org.jboss.as.clustering.jgroups.subsystem.CipherAuthTokenResourceDefinition.Attribute.KEY_STORE;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;

import javax.crypto.Cipher;

import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.CredentialSourceDependency;
import org.jboss.as.clustering.jgroups.auth.CipherAuthToken;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.source.CredentialSource;
import org.wildfly.security.password.interfaces.ClearPassword;

/**
 * @author Paul Ferraro
 */
public class CipherAuthTokenServiceConfigurator extends AuthTokenServiceConfigurator<CipherAuthToken> {

    private volatile SupplierDependency<KeyStore> keyStore;
    private volatile SupplierDependency<CredentialSource> keyCredentialSource;
    private volatile String keyAlias;
    private volatile String transformation;

    public CipherAuthTokenServiceConfigurator(PathAddress address) {
        super(address);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String keyStore = KEY_STORE.resolveModelAttribute(context, model).asString();
        this.keyStore = new ServiceSupplierDependency<>(CommonUnaryRequirement.KEY_STORE.getServiceName(context, keyStore));
        this.keyAlias = KEY_ALIAS.resolveModelAttribute(context, model).asString();
        this.keyCredentialSource = new CredentialSourceDependency(context, KEY_CREDENTIAL, model);
        this.transformation = ALGORITHM.resolveModelAttribute(context, model).asString();
        return super.configure(context, model);
    }

    @Override
    public <V> ServiceBuilder<V> register(ServiceBuilder<V> builder) {
        return super.register(new CompositeDependency(this.keyStore, this.keyCredentialSource).register(builder));
    }

    @Override
    public CipherAuthToken apply(String authValue) {
        KeyStore store = this.keyStore.get();
        String alias = this.keyAlias;
        try {
            if (!store.containsAlias(alias)) {
                throw JGroupsLogger.ROOT_LOGGER.keyEntryNotFound(alias);
            }
            if (!store.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class)) {
                throw JGroupsLogger.ROOT_LOGGER.unexpectedKeyStoreEntryType(alias, KeyStore.PrivateKeyEntry.class.getSimpleName());
            }
            PasswordCredential credential = this.keyCredentialSource.get().getCredential(PasswordCredential.class);
            if (credential == null) {
                throw JGroupsLogger.ROOT_LOGGER.unexpectedCredentialSource();
            }
            ClearPassword password = credential.getPassword(ClearPassword.class);
            if (password == null) {
                throw JGroupsLogger.ROOT_LOGGER.unexpectedCredentialSource();
            }
            KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) store.getEntry(alias, new KeyStore.PasswordProtection(password.getPassword()));
            KeyPair pair = new KeyPair(entry.getCertificate().getPublicKey(), entry.getPrivateKey());
            Cipher cipher = Cipher.getInstance(this.transformation);
            return new CipherAuthToken(cipher, pair, authValue.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
