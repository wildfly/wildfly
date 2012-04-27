/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.security;

import static org.jboss.as.domain.management.DomainManagementLogger.ROOT_LOGGER;
import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;
import static org.jboss.as.domain.management.RealmConfigurationConstants.SUBJECT_CALLBACK_SUPPORTED;

import java.io.IOException;
import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.net.ssl.SSLContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.jboss.as.controller.security.SubjectUserInfo;
import org.jboss.as.domain.management.AuthenticationMechanism;
import org.jboss.as.domain.management.AuthorizingCallbackHandler;
import org.jboss.as.domain.management.CallbackHandlerFactory;
import org.jboss.as.domain.management.CallbackHandlerServiceRegistry;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * The service representing the security realm, this service will be injected into any management interfaces
 * requiring any of the capabilities provided by the realm.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SecurityRealmService implements Service<SecurityRealm>, SecurityRealm, CallbackHandlerServiceRegistry {

    public static final ServiceName BASE_SERVICE_NAME = ServiceName.JBOSS.append("server", "controller", "management", "security_realm");

    private final InjectedValue<SubjectSupplemental> subjectSupplemental = new InjectedValue<SubjectSupplemental>();
    private final InjectedValue<SSLIdentityService> sslIdentity = new InjectedValue<SSLIdentityService>();
    private final InjectedValue<CallbackHandlerFactory> secretCallbackFactory = new InjectedValue<CallbackHandlerFactory>();

    private final String name;
    private final Map<AuthenticationMechanism, CallbackHandlerService> registeredServices = new HashMap<AuthenticationMechanism, CallbackHandlerService>();
    private boolean started = false;

    public SecurityRealmService(String name) {
        this.name = name;
    }

    /*
     * CallbackHandlerServiceRegistry Methods
     */

    public void register(AuthenticationMechanism mechanism, CallbackHandlerService callbackHandler) {
        if (started) {
            throw MESSAGES.registryUpdateAfterStarted();
        }

        synchronized (registeredServices) {
            if (registeredServices.containsKey(mechanism)) {
                throw MESSAGES.callbackHandlerAlreadyRegistered(mechanism.name());
            }
            registeredServices.put(mechanism, callbackHandler);
        }
    }

    public void unregister(AuthenticationMechanism mechanism, CallbackHandlerService callbackHandler) {
        if (started) {
            throw MESSAGES.registryUpdateAfterStarted();
        }

        synchronized (registeredServices) {
            if (registeredServices.containsKey(mechanism) == false || registeredServices.get(mechanism) != callbackHandler) {
                throw MESSAGES.callbackHandlerRegistrationMisMatch();
            }
            registeredServices.remove(mechanism);
        }
    }

    /*
     * Service Methods
     */

    public void start(StartContext context) throws StartException {
        ROOT_LOGGER.debugf("Starting '%s' Security Realm Service", name);
        SecurityRealmRegistry.register(name, getValue());
        started = true;
    }

    public void stop(StopContext context) {
        ROOT_LOGGER.debugf("Stopping '%s' Security Realm Service", name);
        SecurityRealmRegistry.remove(name);
        started = false;
    }

    public SecurityRealmService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public String getName() {
        return name;
    }


    /*
     * SecurityRealm Methods
     */

    public Set<AuthenticationMechanism> getSupportedAuthenticationMechanisms() {
        Set<AuthenticationMechanism> response = new TreeSet<AuthenticationMechanism>();
        response.addAll(registeredServices.keySet());
        return response;
    }

    public Map<String, String> getMechanismConfig(final AuthenticationMechanism mechanism) {
        CallbackHandlerService service = getCallbackHandlerService(mechanism);

        return service.getConfigurationOptions();
    }

    public boolean isReady() {
        for (CallbackHandlerService current : registeredServices.values()) {
            // Only takes one to not be ready for us to return false.
            if (current.isReady() == false) {
                return false;
            }
        }
        return true;
    }

    public AuthorizingCallbackHandler getAuthorizingCallbackHandler(AuthenticationMechanism mechanism) {
        /*
         * The returned AuthorizingCallbackHandler is used for a single authentication request - this means that state can be
         * shared to combine the authentication step and the loading of authorization data.
         */
        final CallbackHandlerService handlerService = getCallbackHandlerService(mechanism);
        return new AuthorizingCallbackHandler() {
            CallbackHandler handler = handlerService.getCallbackHandler();
            Map<String, String> options = handlerService.getConfigurationOptions();
            final boolean subjectCallbackSupported;

            {
                if (options.containsKey(SUBJECT_CALLBACK_SUPPORTED)) {
                    subjectCallbackSupported = Boolean.parseBoolean(options.get(SUBJECT_CALLBACK_SUPPORTED));
                } else {
                    subjectCallbackSupported = false;
                }
            }

            Subject subject;

            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                if (subjectCallbackSupported) {
                    Callback[] newCallbacks = new Callback[callbacks.length + 1];
                    System.arraycopy(callbacks, 0, newCallbacks, 0, callbacks.length);
                    SubjectCallback subjectCallBack = new SubjectCallback();
                    newCallbacks[newCallbacks.length - 1] = subjectCallBack;
                    handler.handle(newCallbacks);
                    subject = subjectCallBack.getSubject();
                } else {
                    handler.handle(callbacks);
                }
            }

            public SubjectUserInfo createSubjectUserInfo(Collection<Principal> userPrincipals) throws IOException {
                Subject subject = this.subject == null ? new Subject() : this.subject;
                Collection<Principal> allPrincipals = subject.getPrincipals();
                for (Principal userPrincipal : userPrincipals) {
                    allPrincipals.add(userPrincipal);
                    allPrincipals.add(new RealmUser(getName(), userPrincipal.getName()));
                }

                SubjectSupplemental subjectSupplemental = getSubjectSupplemental();
                if (subjectSupplemental != null) {
                    subjectSupplemental.supplementSubject(subject);
                }

                return new RealmSubjectUserInfo(subject);
            }
        };
    }

    private CallbackHandlerService getCallbackHandlerService(final AuthenticationMechanism mechanism) {
        if (registeredServices.containsKey(mechanism)) {
            return registeredServices.get(mechanism);
        }
        // As the service is started we do not expect any updates to the registry.

        // We didn't find a service that prefers this mechanism so now search for a service that also supports it.
        for (CallbackHandlerService current : registeredServices.values()) {
            if (current.getSupplementaryMechanisms().contains(mechanism)) {
                return current;
            }
        }

        throw MESSAGES.noCallbackHandlerForMechanism(mechanism.toString(), name);
    }

    /*
     * Injectors
     */

    public InjectedValue<SubjectSupplemental> getSubjectSupplementalInjector() {
        return subjectSupplemental;
    }

    public InjectedValue<SSLIdentityService> getSSLIdentityInjector() {
        return sslIdentity;
    }

    public InjectedValue<CallbackHandlerFactory> getSecretCallbackFactory() {
        return secretCallbackFactory;
    }

    /**
     * Used to obtain the linked SubjectSupplemental if available.
     *
     * @return {@link SubjectSupplemental} The linkes SubjectSupplemental.
     */
    private SubjectSupplemental getSubjectSupplemental() {
        return subjectSupplemental.getOptionalValue();
    }

    public SSLContext getSSLContext() {
        SSLIdentityService service = sslIdentity.getOptionalValue();
        if (service != null) {
            return service.getSSLContext();
        }

        return null;
    }

    public boolean hasTrustStore() {
        SSLIdentityService service;
        return ((service = sslIdentity.getOptionalValue()) != null && service.hasTrustStore());
    }

    public CallbackHandlerFactory getSecretCallbackHandlerFactory() {
        return secretCallbackFactory.getOptionalValue();
    }

    private static class RealmSubjectUserInfo implements SubjectUserInfo {

        private final String userName;
        private final Subject subject;

        private RealmSubjectUserInfo(Subject subject) {
            this.subject = subject;
            Set<RealmUser> users = subject.getPrincipals(RealmUser.class);
            userName = users.isEmpty() ? null : users.iterator().next().getName();
        }

        public String getUserName() {
            return userName;
        }

        public Collection<Principal> getPrincipals() {
            return subject.getPrincipals();
        }

        public Subject getSubject() {
            return subject;
        }

    }
}
