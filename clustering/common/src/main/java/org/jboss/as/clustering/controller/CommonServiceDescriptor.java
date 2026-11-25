/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import java.security.KeyStore;

import javax.net.ssl.SSLContext;
import javax.sql.DataSource;

import org.wildfly.security.credential.store.CredentialStore;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * Service descriptors for clustering dependencies whose providing modules do not yet expose their provided capabilities via descriptor.
 * @author Paul Ferraro
 */
public interface CommonServiceDescriptor {
    UnaryServiceDescriptor<DataSource> DATA_SOURCE = UnaryServiceDescriptor.of("org.wildfly.data-source", DataSource.class);

    UnaryServiceDescriptor<CredentialStore> CREDENTIAL_STORE = UnaryServiceDescriptor.of("org.wildfly.security.credential-store", CredentialStore.class);
    UnaryServiceDescriptor<KeyStore> KEY_STORE = UnaryServiceDescriptor.of("org.wildfly.security.key-store", KeyStore.class);
    UnaryServiceDescriptor<SSLContext> SSL_CONTEXT = UnaryServiceDescriptor.of("org.wildfly.security.ssl-context", SSLContext.class);
}
