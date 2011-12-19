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

import java.lang.reflect.Method;

import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.ee.component.Component;
import org.jboss.as.ee.component.ComponentInstance;
import org.jboss.as.ee.naming.InjectedEENamespaceContextSelector;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.jboss.as.server.CurrentServiceContainer;
import org.jboss.ejb.client.EJBClientContext;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import static org.jboss.as.appclient.logging.AppClientLogger.ROOT_LOGGER;


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
    private final LazyConnectionContextSelector lazyConnectionContextSelector;
    private final CallbackHandler callbackHandler;
    final String hostUrl;

    private Thread thread;
    private ComponentInstance instance;

    public ApplicationClientStartService(final Method mainMethod, final String[] parameters, final String hostUrl, final InjectedEENamespaceContextSelector namespaceContextSelectorInjectedValue, final ClassLoader classLoader, final CallbackHandler callbackHandler) {
        this.mainMethod = mainMethod;
        this.parameters = parameters;
        this.namespaceContextSelectorInjectedValue = namespaceContextSelectorInjectedValue;
        this.classLoader = classLoader;
        this.hostUrl = hostUrl;
        this.callbackHandler = callbackHandler;
        this.lazyConnectionContextSelector = new LazyConnectionContextSelector(hostUrl, callbackHandler);
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {

        thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    final ClassLoader oldTccl = SecurityActions.getContextClassLoader();
                    try {
                        try {
                            SecurityActions.setContextClassLoader(classLoader);
                            EJBClientContext.setSelector(lazyConnectionContextSelector);
                            applicationClientDeploymentServiceInjectedValue.getValue().getDeploymentCompleteLatch().await();
                            NamespaceContextSelector.setDefault(namespaceContextSelectorInjectedValue);

                            //do static injection etc
                            instance = applicationClientComponent.getValue().createInstance();

                            mainMethod.invoke(null, new Object[]{parameters});
                        } catch (Exception e) {
                            ROOT_LOGGER.exceptionRunningAppClient(e, e.getClass().getSimpleName());
                        } finally {
                            SecurityActions.setContextClassLoader(oldTccl);
                        }
                    } finally {
                        CurrentServiceContainer.getServiceContainer().shutdown();
                    }

                } finally {
                    lazyConnectionContextSelector.close();
                }
            }
        });
        thread.start();

    }

    @Override
    public synchronized void stop(final StopContext context) {
        if (instance != null) {
            instance.destroy();
        }
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
}
