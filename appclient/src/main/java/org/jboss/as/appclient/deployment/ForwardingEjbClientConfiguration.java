package org.jboss.as.appclient.deployment;

import java.util.Iterator;

import javax.security.auth.callback.CallbackHandler;

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
}
