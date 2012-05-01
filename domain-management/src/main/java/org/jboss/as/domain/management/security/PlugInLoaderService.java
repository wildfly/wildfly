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

import static org.jboss.as.domain.management.DomainManagementMessages.MESSAGES;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jboss.as.domain.management.plugin.AuthenticationPlugIn;
import org.jboss.as.domain.management.plugin.AuthorizationPlugIn;
import org.jboss.as.domain.management.plugin.Credential;
import org.jboss.as.domain.management.plugin.PlugInProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service responsible for loading plug-ins as needed.
 *
 * This service handles the load requests on-demand and caches the results, this is because a realm could be configured with
 * many plug-ins but will never load more than 2.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class PlugInLoaderService implements Service<PlugInLoaderService> {

    public static final String SERVICE_SUFFIX = "plug-in-loader";

    private final ModelNode plugInModel;
    private List<String> plugInNames = null;
    private final Map<String, List<PlugInProvider>> cachedProviders = new HashMap<String, List<PlugInProvider>>();
    private final Map<String, PlugInProvider> authenticationProviders = new HashMap<String, PlugInProvider>();
    private final Map<String, PlugInProvider> authorizationProviders = new HashMap<String, PlugInProvider>();

    public PlugInLoaderService(final ModelNode plugInModel) {
        this.plugInModel = plugInModel;
    }

    public void start(StartContext context) throws StartException {
        // Convert any configuration into a usable format but don't handle the actual loads.
        List<Property> plugIns = plugInModel.asPropertyList();
        ArrayList<String> knownNames = new ArrayList<String>(plugIns.size());
        for (Property current : plugIns) {
            knownNames.add(current.getName());
        }
        plugInNames = knownNames;
    }

    public void stop(StopContext context) {
        // Clear any cached data so it can be reloaded on next start.
        plugInNames = null;
        cachedProviders.clear();
        authenticationProviders.clear();
        authorizationProviders.clear();
    }

    public PlugInLoaderService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    private List<PlugInProvider> loadPlugInProvider(final String name) {
        synchronized (cachedProviders) {
            List<PlugInProvider> response;
            if (cachedProviders.containsKey(name)) {
                response = cachedProviders.get(name);
            } else {
                List<PlugInProvider> providers = new LinkedList<PlugInProvider>();
                try {
                    for (PlugInProvider current : Module.loadServiceFromCallerModuleLoader(ModuleIdentifier.fromString(name),
                            PlugInProvider.class)) {
                        providers.add(current);
                    }
                } catch (ModuleLoadException e) {
                    throw MESSAGES.unableToLoadPlugInProviders(name, e.getMessage());
                }
                if (providers.size() > 0) {
                    cachedProviders.put(name, providers);
                    response = providers;
                } else {
                    throw MESSAGES.noPlugInProvidersLoaded(name);
                }
            }
            return response;
        }
    }

    public AuthenticationPlugIn<Credential> loadAuthenticationPlugIn(final String name) {
        AuthenticationPlugIn<Credential> response = null;
        synchronized (authenticationProviders) {
            if (authenticationProviders.containsKey(name)) {
                PlugInProvider provider = authenticationProviders.get(name);
                response = provider.loadAuthenticationPlugIn(name);
                if (response == null) {
                    // For some reason the provider that previosly handed this name is no longer handling it.
                    authenticationProviders.remove(name);
                }
            }
            if (response == null) {
                for (String current : plugInNames) {
                    List<PlugInProvider> providerList = loadPlugInProvider(current);
                    for (PlugInProvider currentProvider : providerList) {
                        response = currentProvider.loadAuthenticationPlugIn(name);
                        if (response != null) {
                            authenticationProviders.put(name, currentProvider);
                            break;
                        }
                    }
                }
            }
        }
        if (response == null) {
            throw MESSAGES.noAuthenticationPlugInFound(name);
        }

        return response;
    }

    public AuthorizationPlugIn loadAuthorizationPlugIn(final String name) {
        AuthorizationPlugIn response = null;
        synchronized (authorizationProviders) {
            if (authorizationProviders.containsKey(name)) {
                PlugInProvider provider = authorizationProviders.get(name);
                response = provider.loadAuthorizationPlugIn(name);
                if (response == null) {
                    // For some reason the provider that previosly handed this name is no longer handling it.
                    authorizationProviders.remove(name);
                }
            }
            if (response == null) {
                for (String current : plugInNames) {
                    List<PlugInProvider> providerList = loadPlugInProvider(current);
                    for (PlugInProvider currentProvider : providerList) {
                        response = currentProvider.loadAuthorizationPlugIn(name);
                        if (response != null) {
                            authorizationProviders.put(name, currentProvider);
                            break;
                        }
                    }
                }
            }
        }
        if (response == null) {
            throw MESSAGES.noAuthenticationPlugInFound(name);
        }

        return response;
    }

}
