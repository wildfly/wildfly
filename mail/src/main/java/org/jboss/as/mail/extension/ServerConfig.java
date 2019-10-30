/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.mail.extension;

import java.util.Map;

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
    private final String outgoingSocketBinding;
    private final Credentials credentials;
    private final InjectedValue<ExceptionSupplier<CredentialSource, Exception>> credentialSourceSupplierInjector = new InjectedValue<>();
    private boolean sslEnabled = false;
    private boolean tlsEnabled = false;
    private final Map<String, String> properties;

    public ServerConfig(final String outgoingSocketBinding, final Credentials credentials, boolean ssl, boolean tls, Map<String, String> properties) {
        this.outgoingSocketBinding = outgoingSocketBinding;
        this.credentials = credentials;
        this.sslEnabled = ssl;
        this.tlsEnabled = tls;
        this.properties = properties;
    }

    public String getOutgoingSocketBinding() {
        return outgoingSocketBinding;
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
