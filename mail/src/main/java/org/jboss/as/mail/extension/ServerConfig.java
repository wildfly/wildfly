/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.mail.extension;

import java.util.Map;
import java.util.function.Supplier;

import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.source.CredentialSource;
import org.wildfly.security.password.interfaces.ClearPassword;

/**
 * @author Tomaz Cerar
 * @created 10.8.11 22:50
 */
class ServerConfig {
    private final Supplier<OutboundSocketBinding> outgoingSocketBinding;
    private final Credentials credentials;
    private final InjectedValue<ExceptionSupplier<CredentialSource, Exception>> credentialSourceSupplierInjector = new InjectedValue<>();
    private boolean sslEnabled = false;
    private boolean tlsEnabled = false;
    private final Map<String, String> properties;

    public ServerConfig(final Supplier<OutboundSocketBinding> outgoingSocketBinding, final Credentials credentials, boolean ssl, boolean tls, Map<String, String> properties) {
        this.outgoingSocketBinding = outgoingSocketBinding;
        this.credentials = credentials;
        this.sslEnabled = ssl;
        this.tlsEnabled = tls;
        this.properties = properties;
    }

    public OutboundSocketBinding getOutgoingSocketBinding() {
        return (this.outgoingSocketBinding != null) ? this.outgoingSocketBinding.get() : null;
    }

    public Credentials getCredentials() {
        ExceptionSupplier<CredentialSource, Exception> credentialSourceSupplier = credentialSourceSupplierInjector.getOptionalValue();
        if (credentialSourceSupplier != null) {
            try {
                CredentialSource credentialSource = credentialSourceSupplier.get();
                if (credentialSource == null) {
                    return credentials;
                }
                char[] password = credentialSource.getCredential(PasswordCredential.class).getPassword(ClearPassword.class).getPassword();
                return new Credentials(credentials.getUsername(), new String(password));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            return credentials;
        }
    }

    public boolean isSslEnabled() {
        return sslEnabled;
    }

    public boolean isTlsEnabled() {
        return tlsEnabled;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public Injector<ExceptionSupplier<CredentialSource, Exception>> getCredentialSourceSupplierInjector() {
        return credentialSourceSupplierInjector;
    }
}
