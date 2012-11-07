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

package org.jboss.as.appclient.deployment;

import java.util.Iterator;

import javax.security.auth.callback.CallbackHandler;

import org.jboss.ejb.client.DeploymentNodeSelector;
import org.jboss.ejb.client.EJBClientConfiguration;
import org.xnio.OptionMap;

/**
 * @author Stuart Douglas
 */
public abstract class ForwardingEjbClientConfiguration implements EJBClientConfiguration {

    private final EJBClientConfiguration delegate;

    protected ForwardingEjbClientConfiguration(final EJBClientConfiguration delegate) {
        this.delegate = delegate;
    }

    @Override
    public String getEndpointName() {
        return delegate.getEndpointName();
    }

    @Override
    public OptionMap getEndpointCreationOptions() {
        return delegate.getEndpointCreationOptions();
    }

    @Override
    public OptionMap getRemoteConnectionProviderCreationOptions() {
        return delegate.getRemoteConnectionProviderCreationOptions();
    }

    @Override
    public CallbackHandler getCallbackHandler() {
        return delegate.getCallbackHandler() ;
    }

    @Override
    public Iterator<RemotingConnectionConfiguration> getConnectionConfigurations() {
        return delegate.getConnectionConfigurations();
    }

    @Override
    public Iterator<ClusterConfiguration> getClusterConfigurations() {
        return delegate.getClusterConfigurations();
    }

    @Override
    public ClusterConfiguration getClusterConfiguration(final String clusterName) {
        return delegate.getClusterConfiguration(clusterName);
    }

    @Override
    public long getInvocationTimeout() {
        return delegate.getInvocationTimeout();
    }

    @Override
    public long getReconnectTasksTimeout() {
        return delegate.getReconnectTasksTimeout();
    }

    @Override
    public DeploymentNodeSelector getDeploymentNodeSelector() {
        return delegate.getDeploymentNodeSelector();
    }
}
