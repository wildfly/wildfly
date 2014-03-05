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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.net.ssl.SSLContext;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;

import org.jboss.as.core.security.RealmGroup;
import org.jboss.as.core.security.RealmRole;
import org.jboss.as.core.security.RealmSubjectUserInfo;
import org.jboss.as.core.security.RealmUser;
import org.jboss.as.core.security.SubjectUserInfo;
import org.jboss.as.domain.management.AuthMechanism;
import org.jboss.as.domain.management.AuthorizingCallbackHandler;
import org.jboss.as.domain.management.CallbackHandlerFactory;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedSetValue;
import org.jboss.msc.value.InjectedValue;

/**
 * The service representing the security realm, this service will be injected into any management interfaces
 * requiring any of the capabilities provided by the realm.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class SecurityRealmService implements Service<SecurityRealm>, SecurityRealm {

    public static final String LOADED_USERNAME_KEY = SecurityRealmService.class.getName() + ".LOADED_USERNAME";

    private final InjectedValue<SubjectSupplementalService> subjectSupplemental = new InjectedValue<SubjectSupplementalService>();
    private final InjectedValue<SSLContext> sslContext = new InjectedValue<SSLContext>();

    private final InjectedValue<CallbackHandlerFactory> secretCallbackFactory = new InjectedValue<CallbackHandlerFactory>();
    private final InjectedSetValue<CallbackHandlerService> callbackHandlerServices = new InjectedSetValue<CallbackHandlerService>();

    private final String name;
    private final boolean mapGroupsToRoles;
    private final Map<AuthMechanism, CallbackHandlerService> registeredServices = new HashMap<AuthMechanism, CallbackHandlerService>();

    public SecurityRealmService(String name, boolean mapGroupsToRoles) {
        this.name = name;
        this.mapGroupsToRoles = mapGroupsToRoles;
    }

    /*
     * Service Methods
     */

    public void start(StartContext context) throws StartException {
        ROOT_LOGGER.debugf("Starting '%s' Security Realm Service", name);
        for (CallbackHandlerService current : callbackHandlerServices.getValue()) {
            AuthMechanism mechanism = current.getPreferredMechanism();
            if (registeredServices.containsKey(mechanism)) {
                registeredServices.clear();
                throw MESSAGES.multipleCallbackHandlerForMechanism(mechanism.name());
            }
            registeredServices.put(mechanism, current);
        }
    }

    public void stop(StopContext context) {
        ROOT_LOGGER.debugf("Stopping '%s' Security Realm Service", name);
        registeredServices.clear();
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

    public Set<AuthMechanism> getSupportedAuthenticationMechanisms() {
        Set<AuthMechanism> response = new TreeSet<AuthMechanism>();
        response.addAll(registeredServices.keySet());
        return response;
    }

    public Map<String, String> getMechanismConfig(final AuthMechanism mechanism) {
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

    public AuthorizingCallbackHandler getAuthorizingCallbackHandler(AuthMechanism mechanism) {
        /*
         * The returned AuthorizingCallbackHandler is used for a single authentication request - this means that state can be
         * shared to combine the authentication step and the loading of authorization data.
         */
        final CallbackHandlerService handlerService = getCallbackHandlerService(mechanism);
        final Map<String, Object> sharedState = new HashMap<String, Object>();
        return new AuthorizingCallbackHandler() {
            CallbackHandler handler = handlerService.getCallbackHandler(sharedState);
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
                // For a follow up call just for AuthorizeCallback we don't want to insert a SubjectCallback
                if (subjectCallbackSupported && notAuthorizeCallback(callbacks)) {
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

            private boolean notAuthorizeCallback(Callback[] callbacks) {
                return (callbacks.length == 1 && callbacks[0] instanceof AuthorizeCallback) == false;
            }

            public SubjectUserInfo createSubjectUserInfo(Collection<Principal> userPrincipals) throws IOException {
                Subject subject = this.subject == null ? new Subject() : this.subject;
                Collection<Principal> allPrincipals = subject.getPrincipals();
                if (sharedState.containsKey(LOADED_USERNAME_KEY)) {
                    allPrincipals.add(new RealmUser(getName(), (String) sharedState.get(LOADED_USERNAME_KEY)));
                } else {
                    for (Principal userPrincipal : userPrincipals) {
                        allPrincipals.add(userPrincipal);
                        allPrincipals.add(new RealmUser(getName(), userPrincipal.getName()));
                    }
                }

                SubjectSupplementalService subjectSupplementalService = subjectSupplemental.getOptionalValue();
                if (subjectSupplementalService != null) {
                    SubjectSupplemental subjectSupplemental = subjectSupplementalService.getSubjectSupplemental(sharedState);
                    subjectSupplemental.supplementSubject(subject);
                }

                if (mapGroupsToRoles) {
                    Set<RealmGroup> groups = subject.getPrincipals(RealmGroup.class);
                    Set<RealmRole> roles = new HashSet<RealmRole>(groups.size());
                    for (RealmGroup current : groups) {
                        roles.add(new RealmRole(current.getName()));
                    }
                    subject.getPrincipals().addAll(roles);
                }

                return new RealmSubjectUserInfo(subject);
            }
        };
    }

    private CallbackHandlerService getCallbackHandlerService(final AuthMechanism mechanism) {
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

    public InjectedValue<SubjectSupplementalService> getSubjectSupplementalInjector() {
        return subjectSupplemental;
    }

    public InjectedValue<SSLContext> getSSLContextInjector() {
        return sslContext;
    }

    public InjectedValue<CallbackHandlerFactory> getSecretCallbackFactory() {
        return secretCallbackFactory;
    }

    public InjectedSetValue<CallbackHandlerService> getCallbackHandlerService() {
        return callbackHandlerServices;
    }

    public SSLContext getSSLContext() {
        return sslContext.getOptionalValue();
    }

    public CallbackHandlerFactory getSecretCallbackHandlerFactory() {
        return secretCallbackFactory.getOptionalValue();
    }
}
