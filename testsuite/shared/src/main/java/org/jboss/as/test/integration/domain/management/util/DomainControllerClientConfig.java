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

package org.jboss.as.test.integration.domain.management.util;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.remote.RemoteConnectionProviderFactory;
import org.jboss.threads.JBossThreadFactory;
import org.xnio.OptionMap;

import javax.security.auth.callback.CallbackHandler;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.security.AccessController;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Shared test configuration where all {@linkplain ModelControllerClient}s share a common {@linkplain Endpoint} and
 * {@linkplain java.util.concurrent.Executor}.
 *
 * @author Emanuel Muckenhuber
 */
public class DomainControllerClientConfig implements Closeable {

    private static final String ENDPOINT_NAME = "mgmt-endpoint";

    private static final AtomicInteger executorCount = new AtomicInteger();
    static ExecutorService createDefaultExecutor() {
        final ThreadGroup group = new ThreadGroup("mgmt-client-thread");
        final ThreadFactory threadFactory = new JBossThreadFactory(group, Boolean.FALSE, null, "%G " + executorCount.incrementAndGet() + "-%t", null, null, AccessController.getContext());
        return new ThreadPoolExecutor(4, 4, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(256), threadFactory);
    }

    private final Endpoint endpoint;
    private final ExecutorService executorService;
    private final boolean destroyExecutor;

    DomainControllerClientConfig(final Endpoint endpoint, final ExecutorService executorService, final boolean destroyExecutor) {
        this.endpoint = endpoint;
        this.executorService = executorService;
        this.destroyExecutor = destroyExecutor;
    }

    /**
     * Create a connection wrapper.
     *
     * @param connectionURI the connection URI
     * @param callbackHandler the callback handler
     * @return the connection wrapper
     * @throws IOException
     */
    DomainTestConnection createConnection(final URI connectionURI, final CallbackHandler callbackHandler) throws IOException {
        final ProtocolChannelClient.Configuration configuration = new ProtocolChannelClient.Configuration();
        configuration.setEndpoint(endpoint);
        configuration.setUri(connectionURI);
        final ProtocolChannelClient client = ProtocolChannelClient.create(configuration);
        return new DomainTestConnection(client, callbackHandler, executorService);
    }

    @Override
    public void close() throws IOException {
        if(destroyExecutor) {
            executorService.shutdown();
        }
        if(endpoint != null) try {
            endpoint.close();
        } catch (IOException e) {
            // ignore
        }
        if(destroyExecutor) {
            executorService.shutdownNow();
        }
    }

    public static DomainControllerClientConfig create() throws IOException {
        return create(createDefaultExecutor(), true);
    }

    public static DomainControllerClientConfig create(final ExecutorService executorService) throws IOException {
        return create(executorService, false);
    }

    static DomainControllerClientConfig create(final ExecutorService executorService, boolean destroyExecutor) throws IOException {
        final Endpoint endpoint = Remoting.createEndpoint(ENDPOINT_NAME, OptionMap.EMPTY);
        endpoint.addConnectionProvider("remote", new RemoteConnectionProviderFactory(), OptionMap.EMPTY);
        return new DomainControllerClientConfig(endpoint, executorService, destroyExecutor);
    }

}
