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

package org.jboss.as.controller.client.impl;

import org.jboss.as.controller.client.ModelControllerClientConfiguration;
import org.jboss.threads.JBossThreadFactory;

import javax.net.ssl.SSLContext;
import javax.security.auth.callback.CallbackHandler;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Emanuel Muckenhuber
 */
public class ClientConfigurationImpl implements ModelControllerClientConfiguration {

    private static final int DEFAULT_MAX_THREADS = getSystemProperty("org.jboss.as.controller.client.max-threads", 6);

    // Global count of created pools
    private static final AtomicInteger executorCount = new AtomicInteger();
    static ExecutorService createDefaultExecutor() {
        final ThreadGroup group = new ThreadGroup("management-client-thread");
        final ThreadFactory threadFactory = new JBossThreadFactory(group, Boolean.FALSE, null, "%G " + executorCount.incrementAndGet() + "-%t", null, null, AccessController.getContext());
        return new ThreadPoolExecutor(2, DEFAULT_MAX_THREADS, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), threadFactory);
    }

    private final String address;
    private final int port;
    private final CallbackHandler handler;
    private final Map<String, String> saslOptions;
    private final SSLContext sslContext;
    private final ExecutorService executorService;
    private boolean shutdownExecutor;

    protected ClientConfigurationImpl(String address, int port, CallbackHandler handler, Map<String, String> saslOptions, SSLContext sslContext, ExecutorService executorService) {
        this(address, port, handler, saslOptions, sslContext, executorService, false);
    }

    protected ClientConfigurationImpl(String address, int port, CallbackHandler handler, Map<String, String> saslOptions, SSLContext sslContext, ExecutorService executorService, boolean shutdownExecutor) {
        this.address = address;
        this.port = port;
        this.handler = handler;
        this.saslOptions = saslOptions;
        this.sslContext = sslContext;
        this.executorService = executorService;
        this.shutdownExecutor = shutdownExecutor;
    }

    @Override
    public String getHost() {
        return address;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public CallbackHandler getCallbackHandler() {
        return handler;
    }

    @Override
    public Map<String, String> getSaslOptions() {
        return saslOptions;
    }

    @Override
    public SSLContext getSSLContext() {
        return sslContext;
    }

    @Override
    public int getConnectionTimeout() {
        return 5000;
    }

    @Override
    public ExecutorService getExecutor() {
        return executorService;
    }

    @Override
    public void close() {
        if(shutdownExecutor && executorService != null) {
            executorService.shutdown();
        }
    }

    public static ModelControllerClientConfiguration create(final InetAddress address, final int port) {
        return new ClientConfigurationImpl(address.getHostName(), port, null, null, null, createDefaultExecutor(), true);
    }

    public static ModelControllerClientConfiguration create(final InetAddress address, final int port, final CallbackHandler handler){
        return new ClientConfigurationImpl(address.getHostName(), port, handler, null, null, createDefaultExecutor(), true);
    }

    public static ModelControllerClientConfiguration create(final InetAddress address, final int port, final CallbackHandler handler, final Map<String, String> saslOptions){
        return new ClientConfigurationImpl(address.getHostName(), port, handler, saslOptions, null, createDefaultExecutor(), true);
    }

    public static ModelControllerClientConfiguration create(final String hostName, final int port) throws UnknownHostException {
        return new ClientConfigurationImpl(hostName, port, null, null, null, createDefaultExecutor(), true);
    }

    public static ModelControllerClientConfiguration create(final String hostName, final int port, final CallbackHandler handler) throws UnknownHostException {
        return new ClientConfigurationImpl(hostName, port, handler, null, null, createDefaultExecutor(), true);
    }

    public static ModelControllerClientConfiguration create(final String hostName, final int port, final CallbackHandler handler, final SSLContext sslContext) throws UnknownHostException {
        return new ClientConfigurationImpl(hostName, port, handler, null, sslContext, createDefaultExecutor(), true);
    }

    public static ModelControllerClientConfiguration create(final String hostName, final int port, final CallbackHandler handler, final Map<String, String> saslOptions) throws UnknownHostException {
        return new ClientConfigurationImpl(hostName, port, handler, saslOptions, null, createDefaultExecutor(), true);
    }

    private static int getSystemProperty(final String name, final int defaultValue) {
        final SecurityManager sm = System.getSecurityManager();
        if(sm == null) {
            return Integer.getInteger(name, defaultValue);
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<Integer>() {
                @Override
                public Integer run() {
                    return Integer.getInteger(name, defaultValue);
                }
            });
        }
    }


}
