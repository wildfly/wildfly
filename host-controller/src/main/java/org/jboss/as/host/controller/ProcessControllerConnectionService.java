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

package org.jboss.as.host.controller;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.AccessController;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.process.AsyncProcessControllerClient;
import org.jboss.as.process.ProcessControllerClient;
import org.jboss.as.process.ProcessInfo;
import org.jboss.as.process.ProcessMessageHandler;
import org.jboss.as.process.protocol.ProtocolClient;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.jboss.threads.JBossThreadFactory;

/**
 * @author Emanuel Muckenhuber
 */
class ProcessControllerConnectionService implements Service<AsyncProcessControllerClient> {

    static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("host", "controller", "process-controller-connection");

    private final HostControllerEnvironment environment;
    private final byte[] authCode;
    private volatile AsyncProcessControllerClient client;

    private final InjectedValue<Executor> injectedExecutor = new InjectedValue<Executor>();

    ProcessControllerConnectionService(final HostControllerEnvironment environment, final byte[] authCode) {
        this.environment = environment;
        this.authCode = authCode;
        injectedExecutor.setValue(new Value<Executor>() {
            @Override
            public Executor getValue() throws IllegalStateException, IllegalArgumentException {
                return Executors.newCachedThreadPool();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void start(StartContext context) throws StartException {
        final AsyncProcessControllerClient client;
        try {
            final ProtocolClient.Configuration configuration = new ProtocolClient.Configuration();
            configuration.setReadExecutor(injectedExecutor.getValue());
            configuration.setServerAddress(new InetSocketAddress(environment.getProcessControllerAddress(), environment.getProcessControllerPort().intValue()));
            configuration.setBindAddress(new InetSocketAddress(environment.getHostControllerAddress(), environment.getHostControllerPort()));
            final ThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("ProcessControllerConnection-threads"), Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext());
            configuration.setThreadFactory(threadFactory);
            configuration.setSocketFactory(SocketFactory.getDefault());
            client = AsyncProcessControllerClient.connect(configuration, authCode);
        } catch(IOException e) {
            throw new StartException(e);
        }
        this.client = client;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(StopContext context) {
        final AsyncProcessControllerClient client = this.client;
        this.client = null;
        StreamUtils.safeClose(client);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized AsyncProcessControllerClient getValue() throws IllegalStateException, IllegalArgumentException {
        final AsyncProcessControllerClient client = this.client;
        if(client == null) {
            throw new IllegalStateException();
        }
        return client;
    }

    InjectedValue<Executor> getInjectedExecutor() {
        return injectedExecutor;
    }
}
