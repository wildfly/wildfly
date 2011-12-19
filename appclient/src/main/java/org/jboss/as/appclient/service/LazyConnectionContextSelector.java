package org.jboss.as.appclient.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.appclient.logging.AppClientLogger;
import org.jboss.ejb.client.ContextSelector;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.remoting.IoFutureHelper;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.remote.RemoteConnectionProviderFactory;
import org.xnio.IoFuture;
import org.xnio.OptionMap;
import org.xnio.Options;

/**
 * Selector that does not create a connection until it is actually required.
 *
 * This allows the user to perform a login before the connection is established.
 *
 * @author Stuart Douglas
 */
public class LazyConnectionContextSelector implements ContextSelector<EJBClientContext> {

    private final String hostUrl;
    private final CallbackHandler callbackHandler;

    private Endpoint endpoint;
    private Connection connection;

    private volatile EJBClientContext clientContext;


    public LazyConnectionContextSelector(final String hostUrl, final CallbackHandler callbackHandler) {
        this.hostUrl = hostUrl;
        this.callbackHandler = callbackHandler;
    }

    private synchronized void createConnection() {
        try {
            endpoint = Remoting.createEndpoint("endpoint", OptionMap.EMPTY);
            endpoint.addConnectionProvider("remote", new RemoteConnectionProviderFactory(), OptionMap.create(Options.SSL_ENABLED, Boolean.FALSE));

            // open a connection
            final IoFuture<Connection> futureConnection = endpoint.connect(new URI(hostUrl), OptionMap.create(Options.SASL_POLICY_NOANONYMOUS, Boolean.FALSE), callbackHandler);
            connection = IoFutureHelper.get(futureConnection, 30L, TimeUnit.SECONDS);

            final EJBClientContext ejbClientContext = EJBClientContext.create();
            ejbClientContext.registerConnection(connection);

            this.clientContext = ejbClientContext;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public EJBClientContext getCurrent() {
        if (clientContext == null) {
            synchronized (this) {
                if (clientContext == null) {
                    createConnection();
                }
            }
        }
        return clientContext;
    }

    public synchronized void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (IOException e) {
            AppClientLogger.ROOT_LOGGER.exceptionClosingConnection(e);
        }
        try {
            if (endpoint != null) {
                endpoint.close();
            }
        } catch (IOException e) {
            AppClientLogger.ROOT_LOGGER.exceptionClosingConnection(e);
        }
    }
}
