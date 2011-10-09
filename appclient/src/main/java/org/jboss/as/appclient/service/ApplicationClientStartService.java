/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.appclient.service;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.naming.InjectedEENamespaceContextSelector;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.remoting.IoFutureHelper;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.Registration;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.remote.RemoteConnectionProviderFactory;
import org.xnio.IoFuture;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;


/**
 * Service that is responsible for running an application clients main method, and shutting down the server once it
 * completes
 *
 * @author Stuart Douglas
 */
public class ApplicationClientStartService implements Service<ApplicationClientStartService> {


    public static final ServiceName SERVICE_NAME = ServiceName.of("appClientStart");

    private final InjectedValue<ApplicationClientDeploymentService> applicationClientDeploymentServiceInjectedValue = new InjectedValue<ApplicationClientDeploymentService>();
    private final InjectedValue<Component> applicationClientComponent = new InjectedValue<Component>();
    private final InjectedEENamespaceContextSelector namespaceContextSelectorInjectedValue;
    private final Method mainMethod;
    private final String[] parameters;
    private final ClassLoader classLoader;
    final String hostUrl;

    private Thread thread;

    private final Logger logger = Logger.getLogger(ApplicationClientStartService.class);

    public ApplicationClientStartService(final Method mainMethod, final String[] parameters, final String hostUrl, final InjectedEENamespaceContextSelector namespaceContextSelectorInjectedValue, final ClassLoader classLoader) {
        this.mainMethod = mainMethod;
        this.parameters = parameters;
        this.namespaceContextSelectorInjectedValue = namespaceContextSelectorInjectedValue;
        this.classLoader = classLoader;
        this.hostUrl = hostUrl;
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        try {
            //TODO: this is a complete hack
            //we need a real way of setting up the remote EJB
            ExecutorService executor = Executors.newFixedThreadPool(1, new ThreadFactory() {
                @Override
                public Thread newThread(final Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("App Client Remoting Thread");
                    t.setDaemon(true);
                    return t;
                }
            });


            final Endpoint endpoint = Remoting.createEndpoint("endpoint", executor, OptionMap.EMPTY);
            final Xnio xnio = Xnio.getInstance();
            final Registration registration = endpoint.addConnectionProvider("remote", new RemoteConnectionProviderFactory(xnio), OptionMap.create(Options.SSL_ENABLED, false));


            // open a connection
            final IoFuture<Connection> futureConnection = endpoint.connect(new URI(hostUrl), OptionMap.create(Options.SASL_POLICY_NOANONYMOUS, Boolean.FALSE), new AnonymousCallbackHandler());
            final Connection connection = IoFutureHelper.get(futureConnection, 5, TimeUnit.SECONDS);

            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    ClassLoader oldTccl = SecurityActions.getContextClassLoader();
                    try {
                        SecurityActions.setContextClassLoader(classLoader);

                        EJBClientContext ejbClientContext = EJBClientContext.create();
                        ejbClientContext.registerConnection(connection);
                        applicationClientDeploymentServiceInjectedValue.getValue().getDeploymentCompleteLatch().await();

                        //do static injection etc
                        //TODO: this should be better
                        applicationClientComponent.getValue().createInstance();

                        NamespaceContextSelector.pushCurrentSelector(namespaceContextSelectorInjectedValue);
                        mainMethod.invoke(null, new Object[]{parameters});
                    } catch (InvocationTargetException e) {
                        logger.error(e.getTargetException(), e.getTargetException());
                    } catch (IllegalAccessException e) {
                        logger.error("IllegalAccessException running app client main", e);
                    } catch (InterruptedException e) {
                        logger.error("InterruptedException running app client main", e);
                    } finally {
                        SecurityActions.setContextClassLoader(oldTccl);
                        NamespaceContextSelector.popCurrentSelector();
                        CurrentServiceContainer.getServiceContainer().shutdown();
                    }
                }
            });
            thread.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized void stop(final StopContext context) {
        thread.interrupt();
        thread = null;
    }

    @Override
    public ApplicationClientStartService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<ApplicationClientDeploymentService> getApplicationClientDeploymentServiceInjectedValue() {
        return applicationClientDeploymentServiceInjectedValue;
    }

    public InjectedValue<Component> getApplicationClientComponent() {
        return applicationClientComponent;
    }

    /**
     * User: jpai
     */
    public class AnonymousCallbackHandler implements CallbackHandler {

        @Override
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (Callback current : callbacks) {
                if (current instanceof NameCallback) {
                    NameCallback ncb = (NameCallback) current;
                    ncb.setName("anonymous");
                } else {
                    throw new UnsupportedCallbackException(current);
                }
            }
        }
    }

}
