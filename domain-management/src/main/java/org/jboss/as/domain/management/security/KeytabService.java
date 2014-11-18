/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import static javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag.REQUIRED;
import static org.jboss.as.domain.management.DomainManagementLogger.SECURITY_LOGGER;
import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.jboss.as.controller.services.path.PathEntry;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathManager.Event;
import org.jboss.as.controller.services.path.PathManager.PathEventContext;
import org.jboss.as.domain.management.SubjectIdentity;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * An extension to {@link AbstractKeyManagerService} so that a KeyManager[] can be provided based on a JKS file based key store.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class KeytabService implements Service<KeytabService> {

    private static final boolean IS_IBM = System.getProperty("java.vendor").contains("IBM");
    private static final String KRB5LoginModule = "com.sun.security.auth.module.Krb5LoginModule";
    private static final String IBMKRB5LoginModule = "com.ibm.security.auth.module.Krb5LoginModule";

    private static final CallbackHandler NO_CALLBACK_HANDLER = new CallbackHandler() {

        @Override
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            throw new UnsupportedCallbackException(callbacks[0]);
        }
    };

    private final String principal;
    private final String path;
    private final String relativeTo;
    private final String[] forHosts;
    private final boolean debug;
    private final InjectedValue<PathManager> pathManager = new InjectedValue<PathManager>();

    private Configuration clientConfiguration;
    private Configuration serverConfiguration;

    KeytabService(final String principal, final String path, final String relativeTo, final String[] forHosts, final boolean debug) {
        this.principal = principal;
        this.path = path;
        this.relativeTo = relativeTo;
        this.forHosts = forHosts;
        this.debug = debug;
    }

    /*
     * Service Methods
     */

    @Override
    public KeytabService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public void start(StartContext context) throws StartException {
        String file = path;
        if (relativeTo != null) {
            PathManager pm = pathManager.getValue();

            file = pm.resolveRelativePathEntry(file, relativeTo);
            pm.registerCallback(relativeTo, new org.jboss.as.controller.services.path.PathManager.Callback() {

                @Override
                public void pathModelEvent(PathEventContext eventContext, String name) {
                    if (eventContext.isResourceServiceRestartAllowed() == false) {
                        eventContext.reloadRequired();
                    }
                }

                @Override
                public void pathEvent(Event event, PathEntry pathEntry) {
                    // Service dependencies should trigger a stop and start.
                }
            }, Event.REMOVED, Event.UPDATED);
        }

        try {
            clientConfiguration = createConfiguration(false, file);
            serverConfiguration = createConfiguration(true, file);
        } catch (MalformedURLException e) {
            throw MESSAGES.invalidKeytab(e);
        }
    }

    private Configuration createConfiguration(final boolean isServer, final String keytabLocation) throws MalformedURLException {
        Map<String, Object> options = new HashMap<String, Object>();
        if (debug) {
            options.put("debug", "true");
        }
        options.put("principal", principal);

        final AppConfigurationEntry ace;
        if (IS_IBM) {
            options.put("noAddress", "true");
            options.put("credsType", isServer ? "acceptor" : "initiator");
            options.put("useKeytab", new File(keytabLocation).toURI().toURL().toString());
            ace = new AppConfigurationEntry(IBMKRB5LoginModule, REQUIRED, options);
        } else {
            options.put("storeKey", "true");
            options.put("useKeyTab", "true");
            options.put("keyTab", keytabLocation);
            options.put("isInitiator", isServer ? "false" : "true");

            ace = new AppConfigurationEntry(KRB5LoginModule, REQUIRED, options);
        }

        final AppConfigurationEntry[] aceArray = new AppConfigurationEntry[] { ace };

        return new Configuration() {

            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                assert "KDC".equals(name);

                return aceArray;
            }

        };

    }

    @Override
    public void stop(StopContext context) {
        clientConfiguration = null;
        serverConfiguration = null;
    }

    Injector<PathManager> getPathManagerInjector() {
        return pathManager;
    }

    /*
     * Exposed Methods
     */

    public String getPrincipal() {
        return principal;
    }

    public String[] getForHosts() {
        return forHosts.clone();
    }

    public SubjectIdentity createSubjectIdentity(final boolean isClient) throws LoginException {
        final Subject theSubject = new Subject();

        final LoginContext lc = new LoginContext("KDC", theSubject, NO_CALLBACK_HANDLER, isClient ? clientConfiguration : serverConfiguration);
        lc.login();


        return new SubjectIdentity() {

            volatile boolean available = true;

            @Override
            public Subject getSubject() {
                assertAvailable();
                return theSubject;
            }

            @Override
            public void logout() {
                assertAvailable();
                try {
                    lc.logout();
                } catch (LoginException e) {
                    SECURITY_LOGGER.trace("Unable to logout.", e);
                }
            }

            private void assertAvailable() {
                if (available == false) {
                    throw MESSAGES.subjectIdentityLoggedOut();
                }
            }

        };
    }


    public static final class ServiceUtil {

        private ServiceUtil() {
        }

        public static ServiceName createServiceName(final String realmName, final String principal) {
            return KeytabIdentityFactoryService.ServiceUtil.createServiceName(realmName).append(principal);
        }

        public static ServiceBuilder<?> addDependency(ServiceBuilder<?> sb, Injector<KeytabService> injector,
                String realmName, String principal) {
            ServiceBuilder.DependencyType type = ServiceBuilder.DependencyType.REQUIRED;
            sb.addDependency(type, createServiceName(realmName, principal), KeytabService.class, injector);

            return sb;
        }

    }

}
