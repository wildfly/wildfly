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

package org.jboss.as.domain.management.security;

import static org.jboss.as.domain.management.DomainManagementLogger.SECURITY_LOGGER;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.sasl.AuthorizeCallback;

import org.jboss.as.domain.management.AuthMechanism;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * A CallbackHandler for Client Cert authentication. Currently no Callbacks are supported but later this may be expanded for
 * additional verification.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ClientCertCallbackHandler implements Service<CallbackHandlerService>, CallbackHandlerService, CallbackHandler {

    private static final String SERVICE_SUFFIX = "client_cert";

    ClientCertCallbackHandler() {
    }

    /*
     * Service Methods
     */

    public CallbackHandlerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public void start(StartContext context) throws StartException {
    }

    public void stop(StopContext context) {
    }

    /*
     * CallbackHandlerService Methods
     */

    public AuthMechanism getPreferredMechanism() {
        return AuthMechanism.CLIENT_CERT;
    }

    public Set<AuthMechanism> getSupplementaryMechanisms() {
        return Collections.emptySet();
    }

    public Map<String, String> getConfigurationOptions() {
        return Collections.emptyMap();
    }

    public boolean isReady() {
        return true;
    }

    public CallbackHandler getCallbackHandler(Map<String, Object> sharedState) {
        return this;
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback current : callbacks) {
            if (current instanceof AuthorizeCallback) {
                AuthorizeCallback acb = (AuthorizeCallback) current;
                boolean authorized = acb.getAuthenticationID().equals(acb.getAuthorizationID());
                if (authorized == false) {
                    SECURITY_LOGGER.tracef(
                            "Checking 'AuthorizeCallback', authorized=false, authenticationID=%s, authorizationID=%s.",
                            acb.getAuthenticationID(), acb.getAuthorizationID());
                }
                acb.setAuthorized(authorized);
            } else {
                throw new UnsupportedCallbackException(current);
            }
        }
    }

    public static final class ServiceUtil {

        private ServiceUtil() {
        }

        public static ServiceName createServiceName(final String realmName) {
            return SecurityRealm.ServiceUtil.createServiceName(realmName).append(SERVICE_SUFFIX);
        }
    }
}
