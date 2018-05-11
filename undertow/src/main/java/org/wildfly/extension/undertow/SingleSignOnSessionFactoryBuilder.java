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

package org.wildfly.extension.undertow;

import static org.wildfly.extension.undertow.ApplicationSecurityDomainSingleSignOnDefinition.Attribute.CREDENTIAL;
import static org.wildfly.extension.undertow.ApplicationSecurityDomainSingleSignOnDefinition.Attribute.KEY_ALIAS;
import static org.wildfly.extension.undertow.ApplicationSecurityDomainSingleSignOnDefinition.Attribute.KEY_STORE;
import static org.wildfly.extension.undertow.ApplicationSecurityDomainSingleSignOnDefinition.Attribute.SSL_CONTEXT;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.CredentialSourceDependency;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.source.CredentialSource;
import org.wildfly.security.http.util.sso.DefaultSingleSignOnSessionFactory;
import org.wildfly.security.http.util.sso.SingleSignOnManager;
import org.wildfly.security.http.util.sso.SingleSignOnSessionFactory;
import org.wildfly.security.password.interfaces.ClearPassword;

/**
 * @author Paul Ferraro
 */
public class SingleSignOnSessionFactoryBuilder extends SingleSignOnSessionFactoryServiceNameProvider implements ResourceServiceBuilder<SingleSignOnSessionFactory>, Value<SingleSignOnSessionFactory> {

    private final ValueDependency<SingleSignOnManager> manager;

    private volatile ValueDependency<KeyStore> keyStore;
    private volatile ValueDependency<SSLContext> sslContext;
    private volatile SupplierDependency<CredentialSource> credentialSource;
    private volatile String keyAlias;

    public SingleSignOnSessionFactoryBuilder(String securityDomainName) {
        super(securityDomainName);
        this.manager = new InjectedValueDependency<>(new SingleSignOnManagerServiceNameProvider(securityDomainName), SingleSignOnManager.class);
    }

    @Override
    public Builder<SingleSignOnSessionFactory> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String keyStore = KEY_STORE.resolveModelAttribute(context, model).asString();
        this.keyStore = new InjectedValueDependency<>(CommonUnaryRequirement.KEY_STORE.getServiceName(context, keyStore), KeyStore.class);
        this.keyAlias = KEY_ALIAS.resolveModelAttribute(context, model).asString();
        this.credentialSource = new CredentialSourceDependency(context, CREDENTIAL, model);
        Optional<String> sslContext = ModelNodes.optionalString(SSL_CONTEXT.resolveModelAttribute(context, model));
        this.sslContext = sslContext.map(value -> new InjectedValueDependency<>(CommonUnaryRequirement.SSL_CONTEXT.getServiceName(context, value), SSLContext.class)).orElse(null);
        return this;
    }

    @Override
    public ServiceBuilder<SingleSignOnSessionFactory> build(ServiceTarget target) {
        ServiceBuilder<SingleSignOnSessionFactory> builder = target.addService(this.getServiceName(), new ValueService<>(this));
        return new CompositeDependency(this.manager, this.keyStore, this.credentialSource, this.sslContext).register(builder);
    }

    @Override
    public SingleSignOnSessionFactory getValue() {
        KeyStore store = this.keyStore.getValue();
        String alias = this.keyAlias;
        CredentialSource source = this.credentialSource.get();
        try {
            if (!store.containsAlias(alias)) {
                throw UndertowLogger.ROOT_LOGGER.missingKeyStoreEntry(alias);
            }
            if (!store.entryInstanceOf(alias, KeyStore.PrivateKeyEntry.class)) {
                throw UndertowLogger.ROOT_LOGGER.keyStoreEntryNotPrivate(alias);
            }
            PasswordCredential credential = source.getCredential(PasswordCredential.class);
            if (credential == null) {
                throw UndertowLogger.ROOT_LOGGER.missingCredential(source.toString());
            }
            ClearPassword password = credential.getPassword(ClearPassword.class);
            if (password == null) {
                throw UndertowLogger.ROOT_LOGGER.credentialNotClearPassword(credential.toString());
            }
            KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) store.getEntry(alias, new KeyStore.PasswordProtection(password.getPassword()));
            KeyPair keyPair = new KeyPair(entry.getCertificate().getPublicKey(), entry.getPrivateKey());
            Optional<SSLContext> context = Optional.ofNullable(this.sslContext).map(dependency -> dependency.getValue());
            return new DefaultSingleSignOnSessionFactory(this.manager.getValue(), keyPair, connection -> context.ifPresent(ctx -> connection.setSSLSocketFactory(ctx.getSocketFactory())));
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
