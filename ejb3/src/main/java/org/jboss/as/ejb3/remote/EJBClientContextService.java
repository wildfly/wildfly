/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ejb3.remote;

import static java.security.AccessController.doPrivileged;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Properties;

import org.jboss.as.ejb3.subsystem.EJBClientConfiguratorService;
import org.jboss.ejb.client.ClusterNodeSelector;
import org.jboss.ejb.client.DeploymentNodeSelector;
import org.jboss.ejb.client.EJBClientCluster;
import org.jboss.ejb.client.EJBClientConnection;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.ejb.client.EJBTransportProvider;
import org.jboss.ejb.client.legacy.JBossEJBProperties;
import org.jboss.ejb.client.legacy.LegacyPropertiesConfiguration;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.security.auth.client.AuthenticationContext;

/**
 * The EJB client context service.
 *
 * @author Stuart Douglas
 * @author <a href=mailto:tadamski@redhat.com>Tomasz Adamski</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class EJBClientContextService implements Service<EJBClientContextService> {

    public static final ServiceName APP_CLIENT_URI_SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "ejbClientContext", "appClientUri");
    public static final ServiceName APP_CLIENT_EJB_PROPERTIES_SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "ejbClientContext", "appClientEjbProperties");

    private static final ServiceName BASE_SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "ejbClientContext");

    public static final ServiceName DEPLOYMENT_BASE_SERVICE_NAME = BASE_SERVICE_NAME.append("deployment");

    public static final ServiceName DEFAULT_SERVICE_NAME = BASE_SERVICE_NAME.append("default");

    private EJBClientContext clientContext;

    private final InjectedValue<EJBClientConfiguratorService> configuratorServiceInjector = new InjectedValue<>();
    private final InjectedValue<EJBTransportProvider> localProviderInjector = new InjectedValue<>();
    private final InjectedValue<RemotingProfileService> profileServiceInjector = new InjectedValue<>();
    private InjectedValue<URI> appClientUri = new InjectedValue<>();
    private InjectedValue<String> appClientEjbProperties = new InjectedValue<>();
    /**
     * TODO: possibly move to using a per-thread solution for embedded support
     */
    private final boolean makeGlobal;
    private long invocationTimeout;
    private DeploymentNodeSelector deploymentNodeSelector;
    private List<EJBClientCluster> clientClusters = null;
    private AuthenticationContext clustersAuthenticationContext = null;
    private List<EJBClientInterceptor> clientInterceptors = null;

    public EJBClientContextService(final boolean makeGlobal) {
        this.makeGlobal = makeGlobal;
    }

    public EJBClientContextService() {
        this.makeGlobal = false;
    }

    public void start(final StartContext context) throws StartException {
        final EJBClientContext.Builder builder = new EJBClientContext.Builder();


        // apply subsystem-level configuration that applies to all EJB client contexts
        configuratorServiceInjector.getValue().accept(builder);

        builder.setInvocationTimeout(invocationTimeout);

        final EJBTransportProvider localTransport = localProviderInjector.getOptionalValue();
        if (localTransport != null) {
            builder.addTransportProvider(localTransport);
        }

        final RemotingProfileService profileService = profileServiceInjector.getOptionalValue();
        if (profileService != null) for (RemotingProfileService.ConnectionSpec spec : profileService.getConnectionSpecs()) {
            final EJBClientConnection.Builder connBuilder = new EJBClientConnection.Builder();
            connBuilder.setDestination(spec.getInjector().getValue().getDestinationUri());
            // connBuilder.setConnectionTimeout(timeout);
            builder.addClientConnection(connBuilder.build());
        }
        if(appClientUri.getOptionalValue() != null) {
            final EJBClientConnection.Builder connBuilder = new EJBClientConnection.Builder();
            connBuilder.setDestination(appClientUri.getOptionalValue());
            builder.addClientConnection(connBuilder.build());
        }

        if (clientClusters != null) {
            boolean firstSelector = true;
            for (EJBClientCluster clientCluster : clientClusters) {
                builder.addClientCluster(clientCluster);
                ClusterNodeSelector selector = clientCluster.getClusterNodeSelector();
                if (firstSelector && selector != null) {
                    builder.setClusterNodeSelector(selector);
                    // Currently only one selector is supported per client context
                    firstSelector = false;
                }
            }
        }

        if (deploymentNodeSelector != null) {
            builder.setDeploymentNodeSelector(deploymentNodeSelector);
        }

        if(appClientEjbProperties.getOptionalValue() != null) {
            setupEjbClientProps(appClientEjbProperties.getOptionalValue());
            LegacyPropertiesConfiguration.configure(builder);
        }

        if (clientInterceptors != null) {
            for (EJBClientInterceptor clientInterceptor : clientInterceptors) {
                builder.addInterceptor(clientInterceptor);
            }
        }

        clientContext = builder.build();
        if (makeGlobal) {
            doPrivileged((PrivilegedAction<Void>) () -> {
                EJBClientContext.getContextManager().setGlobalDefault(clientContext);
                return null;
            });
        }
    }

    public void stop(final StopContext context) {
        clientContext = null;
        if (makeGlobal) {
            doPrivileged((PrivilegedAction<Void>) () -> {
                EJBClientContext.getContextManager().setGlobalDefault(null);
                return null;
            });
        }
    }


    private void setupEjbClientProps(String connectionPropertiesUrl) throws StartException {

        try {
            final File file = new File(connectionPropertiesUrl);
            final URL url;
            if (file.exists()) {
                url = file.toURI().toURL();
            } else {
                url = new URL(connectionPropertiesUrl);
            }
            Properties properties = new Properties();
            InputStream stream = null;
            try {
                stream = url.openStream();
                properties.load(stream);
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        //ignore
                    }
                }
            }
            JBossEJBProperties ejbProps = JBossEJBProperties.fromProperties(connectionPropertiesUrl, properties);
            JBossEJBProperties.getContextManager().setGlobalDefault(ejbProps);
        } catch (Exception e) {
            throw new StartException(e);
        }

    }

    public EJBClientContextService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public EJBClientContext getClientContext() {
        return clientContext;
    }

    public InjectedValue<EJBClientConfiguratorService> getConfiguratorServiceInjector() {
        return configuratorServiceInjector;
    }

    public InjectedValue<EJBTransportProvider> getLocalProviderInjector() {
        return localProviderInjector;
    }

    public InjectedValue<RemotingProfileService> getProfileServiceInjector() {
        return profileServiceInjector;
    }


    public InjectedValue<URI> getAppClientUri() {
        return appClientUri;
    }

    public InjectedValue<String> getAppClientEjbProperties() {
        return appClientEjbProperties;
    }

    public void setInvocationTimeout(final long invocationTimeout) {
        this.invocationTimeout = invocationTimeout;
    }

    public void setDeploymentNodeSelector(final DeploymentNodeSelector deploymentNodeSelector) {
        this.deploymentNodeSelector = deploymentNodeSelector;
    }

    public void setClientClusters(final List<EJBClientCluster> clientClusters) {
        this.clientClusters = clientClusters;
    }

    public void setClustersAuthenticationContext(final AuthenticationContext clustersAuthenticationContext) {
        this.clustersAuthenticationContext = clustersAuthenticationContext;
    }

    public void setClientInterceptors(final List<EJBClientInterceptor> clientInterceptors) {
        this.clientInterceptors = clientInterceptors;
    }

    public AuthenticationContext getClustersAuthenticationContext() {
        return clustersAuthenticationContext;
    }
}
