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
package org.jboss.as.remoting;

import java.io.File;
import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * The service to make the RealmAuthenticationProvider available.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class RealmAuthenticationProviderService implements Service<RealmAuthenticationProvider> {

    private final InjectedValue<SecurityRealm> securityRealmInjectedValue = new InjectedValue<SecurityRealm>();
    private final InjectedValue<CallbackHandler> serverCallbackValue = new InjectedValue<CallbackHandler>();
    private final InjectedValue<String> tmpDirValue = new InjectedValue<String>();

    private volatile RealmAuthenticationProvider realmAuthenticationProvider = null;
    /** The base name of the AuthenticationProvider service */
    private static final ServiceName BASE_NAME = RemotingServices.REMOTING_BASE.append("authentication_provider");

    public static ServiceName createName(String connectorName) {
        return BASE_NAME.append(connectorName);
    }

    public void start(StartContext startContext) throws StartException {
        String path = tmpDirValue.getValue();
        File authDir = new File(path, "auth");
        if (authDir.exists()) {
            if (authDir.isDirectory() == false) {
                throw new StartException("Unable to create tmp dir for auth tokens as file already exists.");
            }
        } else if (authDir.mkdirs() == false) {
            throw new StartException("Unable to create auth dir.");
        } else {
            // As a precaution make perms user restricted for directories created (if the OS allows)
            authDir.setWritable(false, false);
            authDir.setWritable(true, true);
            authDir.setReadable(false, false);
            authDir.setReadable(true, true);
            authDir.setExecutable(false, false);
            authDir.setExecutable(true, true);
        }

        realmAuthenticationProvider = new RealmAuthenticationProvider(securityRealmInjectedValue.getOptionalValue(), serverCallbackValue.getOptionalValue(), authDir.getAbsolutePath());
    }

    public void stop(StopContext stopContext) {
        realmAuthenticationProvider = null;
    }

    public RealmAuthenticationProvider getValue() throws IllegalStateException, IllegalArgumentException {
        return realmAuthenticationProvider;
    }

    public InjectedValue<SecurityRealm> getSecurityRealmInjectedValue() {
        return securityRealmInjectedValue;
    }

    public InjectedValue<CallbackHandler> getServerCallbackValue() {
        return serverCallbackValue;
    }

    public InjectedValue<String> getTmpDirValue() {
     return tmpDirValue;
    }
}
