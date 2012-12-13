/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.embedded;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;

import javax.naming.Context;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Indirection to the {@link StandaloneServer}; used to encapsulate access to the underlying embedded AS Server instance in a
 * manner that does not directly link this class. Necessary to avoid {@link ClassCastException} when this class is loaded by the
 * application {@link ClassLoader} (or any other hierarchical CL) while the server is loaded by a modular environment.
 *
 * @author <a href="mailto:alr@jboss.org">Andrew Lee Rubinger</a>
 * @author Thomas.Diesler@jboss.com
 */
final class StandaloneServerIndirection implements StandaloneServer {

    private final Object standaloneServer;
    private final Method methodStart;
    private final Method methodStop;
    private final Method methodGetService;
    private final Method methodGetModelControllerClient;

    StandaloneServerIndirection(Object standaloneServer) {
        this.standaloneServer = standaloneServer;

        // Get a handle on the {@link StandaloneServer} methods
        final Class<?> standaloneServerClass = standaloneServer.getClass();
        try {
            methodStart = standaloneServerClass.getMethod("start");
            methodStop = standaloneServerClass.getMethod("stop");
            methodGetService = standaloneServerClass.getMethod("getService", ServiceName.class);
            methodGetModelControllerClient = standaloneServerClass.getMethod("getModelControllerClient");
        } catch (final NoSuchMethodException nsme) {
            throw new RuntimeException("Could not get a handle to the method to invoke upon the server", nsme);
        }
    }

    @Override
    public void start()  {
        invokeOnServer(methodStart);
    }

    @Override
    public void stop()  {
        invokeOnServer(methodStop);
    }

    @Override
    public ServiceController<?> getService(ServiceName serviceName) {
        return (ServiceController<?>) invokeOnServer(methodGetService, serviceName);
    }

    @Override
    public ModelControllerClient getModelControllerClient()  {
        return (ModelControllerClient) invokeOnServer(methodGetModelControllerClient);
    }

    @Override
    public void deploy(File file)  {
        throw new UnsupportedOperationException();
    }

    @Override
    public void undeploy(File file) throws ExecutionException, InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Context getContext() {
        throw new UnsupportedOperationException();
    }

    private Object invokeOnServer(final Method method, Object... args) {
        try {
            SecurityActions.setAccessible(method);
            return method.invoke(standaloneServer, args);
        } catch (RuntimeException rte) {
            throw rte;
        } catch (Exception ex) {
            Throwable cause = ex;
            if (ex instanceof InvocationTargetException) {
                cause = ((InvocationTargetException)ex).getCause();
            }
            throw new IllegalStateException(cause);
        }
    }
}
