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

import static org.jboss.as.clustering.jgroups.subsystem.EncryptProtocolResourceDefinition.Attribute.*;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.stream.Stream;

import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.CredentialSourceDependency;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.clustering.jgroups.protocol.EncryptProtocol;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jgroups.protocols.EncryptBase;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.source.CredentialSource;
import org.wildfly.security.password.interfaces.ClearPassword;

/**
 * @author Paul Ferraro
 */
public class EncryptProtocolConfigurationBuilder<P extends EncryptBase & EncryptProtocol> extends ProtocolConfigurationBuilder<P> {

    private volatile ValueDependency<KeyStore> keyStore;
    private volatile ValueDependency<CredentialSource> credentialSource;
    private volatile String keyAlias;

    public EncryptProtocolConfigurationBuilder(PathAddress address) {
        super(address);
    }

    @Override
    public Builder<ProtocolConfiguration<P>> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String keyStore = KEY_STORE.resolveModelAttribute(context, model).asString();
        this.keyStore = new InjectedValueDependency<>(CommonUnaryRequirement.KEY_STORE.getServiceName(context, keyStore), KeyStore.class);
        this.keyAlias = KEY_ALIAS.resolveModelAttribute(context, model).asString();
        this.credentialSource = new CredentialSourceDependency(context, CREDENTIAL, model);
        return super.configure(context, model);
    }

    @Override
    public ServiceBuilder<ProtocolConfiguration<P>> build(ServiceTarget target) {
        ServiceBuilder<ProtocolConfiguration<P>> builder = super.build(target);
        Stream.of(this.keyStore, this.credentialSource).forEach(dependency -> dependency.register(builder));
        return builder;
    }

    @Override
    public void accept(P protocol) {
        KeyStore store = this.keyStore.getValue();
        String alias = this.keyAlias;
        try {
            if (!store.containsAlias(alias)) {
                throw JGroupsLogger.ROOT_LOGGER.keyEntryNotFound(alias);
            }
            PasswordCredential credential = this.credentialSource.getValue().getCredential(PasswordCredential.class);
            if (credential == null) {
                throw JGroupsLogger.ROOT_LOGGER.unexpectedCredentialSource();
            }
            ClearPassword password = credential.getPassword(ClearPassword.class);
            if (password == null) {
                throw JGroupsLogger.ROOT_LOGGER.unexpectedCredentialSource();
            }
            protocol.setKeyStore(store);
            protocol.setKeyAlias(alias);
            protocol.setKeyPassword(new KeyStore.PasswordProtection(password.getPassword()));
        } catch (KeyStoreException | IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
