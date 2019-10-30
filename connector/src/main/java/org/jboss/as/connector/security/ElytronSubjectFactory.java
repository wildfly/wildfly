/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.connector.security;

import org.ietf.jgss.GSSName;
import org.jboss.as.connector._private.Capabilities;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.jca.core.spi.security.SubjectFactory;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.wildfly.security.auth.callback.CredentialCallback;
import org.wildfly.security.auth.client.AuthenticationConfiguration;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.auth.client.AuthenticationContextConfigurationClient;
import org.wildfly.security.auth.principal.NamePrincipal;
import org.wildfly.security.credential.GSSKerberosCredential;
import org.wildfly.security.manager.WildFlySecurityManager;

import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.kerberos.KerberosPrincipal;

import static org.jboss.as.connector.logging.ConnectorLogger.ROOT_LOGGER;

import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * An Elytron based {@link SubjectFactory} implementation. It uses an {@link AuthenticationContext} to select a configuration
 * that matches the resource {@link URI} to obtain the username and password pair that will be inserted into the constructed
 * {@link Subject} instance.
 *
 * @author Flavia Rainone
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class ElytronSubjectFactory implements SubjectFactory, Capabilities {

    private static final RuntimeCapability<Void> AUTHENTICATION_CONTEXT_RUNTIME_CAPABILITY = RuntimeCapability
            .Builder.of(AUTHENTICATION_CONTEXT_CAPABILITY, true, AuthenticationContext.class)
            .build();

    private static final AuthenticationContextConfigurationClient AUTH_CONFIG_CLIENT =
            AccessController.doPrivileged(AuthenticationContextConfigurationClient.ACTION);

    private final AuthenticationContext authenticationContext;
    private URI targetURI;

    /**
     * Constructor
     */
    public ElytronSubjectFactory() {
        this(null, null);
    }

    /**
     * Constructor.
     *
     * @param targetURI the {@link URI} of the target.
     */
    public ElytronSubjectFactory(final AuthenticationContext authenticationContext, final URI targetURI) {
        this.authenticationContext = authenticationContext;
        this.targetURI = targetURI;
    }

    /**
     * {@inheritDoc}
     */
    public Subject createSubject() {
        // If a authenticationContext was defined on the subsystem use that context, otherwise use capture the current
        // configuration.
        final Subject subject = this.createSubject(getAuthenticationContext());
        if (ROOT_LOGGER.isTraceEnabled()) {
            ROOT_LOGGER.subject(subject, Integer.toHexString(System.identityHashCode(subject)));
        }
        return subject;
    }

    /**
     * {@inheritDoc}
     */
    public Subject createSubject(final String authenticationContextName) {
        AuthenticationContext context;
        if (authenticationContextName != null && !authenticationContextName.isEmpty()) {
            final ServiceContainer container = this.currentServiceContainer();
            final ServiceName authContextServiceName = AUTHENTICATION_CONTEXT_RUNTIME_CAPABILITY.getCapabilityServiceName(authenticationContextName);
            context = (AuthenticationContext) container.getRequiredService(authContextServiceName).getValue();
        }
        else {
            context = getAuthenticationContext();
        }
        final Subject subject = this.createSubject(context);
        if (ROOT_LOGGER.isTraceEnabled()) {
            ROOT_LOGGER.subject(subject, Integer.toHexString(System.identityHashCode(subject)));
        }
        return subject;
    }

    /**
     * Create a {@link Subject} with the principal and password credential obtained from the authentication configuration
     * that matches the target {@link URI}.
     *
     * @param authenticationContext the {@link AuthenticationContext} used to select a configuration that matches the
     *                              target {@link URI}.
     * @return the constructed {@link Subject}. It contains a single principal and a {@link PasswordCredential}.
     */
    private Subject createSubject(final AuthenticationContext authenticationContext) {
        final AuthenticationConfiguration configuration = AUTH_CONFIG_CLIENT.getAuthenticationConfiguration(this.targetURI, authenticationContext);
        final CallbackHandler handler = AUTH_CONFIG_CLIENT.getCallbackHandler(configuration);
        final NameCallback nameCallback = new NameCallback("Username: ");
        final PasswordCallback passwordCallback = new PasswordCallback("Password: ", false);
        final CredentialCallback credentialCallback = new CredentialCallback(GSSKerberosCredential.class);
        try {
            handler.handle(new Callback[]{nameCallback, passwordCallback, credentialCallback});
            Subject subject = new Subject();
            // if a GSSKerberosCredential was found, add the enclosed GSSCredential and KerberosTicket to the private set in the Subject.
            if (credentialCallback.getCredential() != null) {
                GSSKerberosCredential kerberosCredential = GSSKerberosCredential.class.cast(credentialCallback.getCredential());
                this.addPrivateCredential(subject, kerberosCredential.getKerberosTicket());
                this.addPrivateCredential(subject, kerberosCredential.getGssCredential());
                // use the GSSName to build a kerberos principal and set it in the Subject.
                GSSName gssName = kerberosCredential.getGssCredential().getName();
                subject.getPrincipals().add(new KerberosPrincipal(gssName.toString()));
            }
            // use the name from the callback, if available, to build a principal and set it in the Subject.
            if (nameCallback.getName() != null) {
                subject.getPrincipals().add(new NamePrincipal(nameCallback.getName()));
            }
            // use the password from the callback, if available, to build a credential and set it as a private credential in the Subject.
            if (passwordCallback.getPassword() != null) {
                this.addPrivateCredential(subject, new PasswordCredential(nameCallback.getName(), passwordCallback.getPassword()));
            }
            return subject;
        } catch(Exception e) {
            throw new SecurityException(e);
        }
    }

    /**
     * Get a reference to the current {@link ServiceContainer}.
     *
     * @return a reference to the current {@link ServiceContainer}.
     */
    private ServiceContainer currentServiceContainer() {
        if(WildFlySecurityManager.isChecking()) {
            return AccessController.doPrivileged(CurrentServiceContainer.GET_ACTION);
        }
        return CurrentServiceContainer.getServiceContainer();
    }

    /**
     * Add the specified credential to the subject's private credentials set.
     *
     * @param subject the {@link Subject} to add the credential to.
     * @param credential a reference to the credential.
     */
    private void addPrivateCredential(final Subject subject, final Object credential) {
        if (!WildFlySecurityManager.isChecking()) {
            subject.getPrivateCredentials().add(credential);
        }
        else {
            AccessController.doPrivileged((PrivilegedAction<Void>) () -> {
                subject.getPrivateCredentials().add(credential);
                return null;
            });
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("ElytronSubjectFactory@").append(Integer.toHexString(System.identityHashCode(this)));
        sb.append("]");

        return sb.toString();
    }

    private AuthenticationContext getAuthenticationContext() {
        return authenticationContext == null ? AuthenticationContext.captureCurrent() : authenticationContext;
    }
}