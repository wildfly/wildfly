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

import java.security.AccessControlContext;
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

import static java.lang.System.getProperty;
import static java.lang.System.getSecurityManager;
import static java.security.AccessController.doPrivileged;

/**
 * @author Emanuel Muckenhuber
 */
public class ClientConfigurationImpl implements ModelControllerClientConfiguration {

    private static final int DEFAULT_MAX_THREADS = getSystemProperty("org.jboss.as.controller.client.max-threads", 6);
    private static final int DEFAULT_CONNECTION_TIMEOUT = 5000;

    // Global count of created pools
    private static final AtomicInteger executorCount = new AtomicInteger();
    static ExecutorService createDefaultExecutor() {
        final ThreadGroup group = new ThreadGroup("management-client-thread");
        final ThreadFactory threadFactory = new JBossThreadFactory(group, Boolean.FALSE, null, "%G " + executorCount.incrementAndGet() + "-%t", null, null, doPrivileged(new PrivilegedAction<AccessControlContext>() {
            public AccessControlContext run() {
                return AccessController.getContext();
            }
        }));
        return new ThreadPoolExecutor(2, DEFAULT_MAX_THREADS, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), threadFactory);
    }

    private final String address;
    private final int port;
    private final CallbackHandler handler;
    private final Map<String, String> saslOptions;
    private final SSLContext sslContext;
    private final ExecutorService executorService;
    private final String protocol;
    private boolean shutdownExecutor;
    private final int connectionTimeout;

    protected ClientConfigurationImpl(String address, int port, CallbackHandler handler, Map<String, String> saslOptions, SSLContext sslContext, ExecutorService executorService, final String protocol) {
        this(address, port, handler, saslOptions, sslContext, executorService, false, DEFAULT_CONNECTION_TIMEOUT, protocol);
    }

    protected ClientConfigurationImpl(String address, int port, CallbackHandler handler, Map<String, String> saslOptions, SSLContext sslContext, ExecutorService executorService, boolean shutdownExecutor, final String protocol) {
        this(address, port, handler, saslOptions, sslContext, executorService, shutdownExecutor, DEFAULT_CONNECTION_TIMEOUT, protocol);
    }

    protected ClientConfigurationImpl(String address, int port, CallbackHandler handler, Map<String, String> saslOptions, SSLContext sslContext, ExecutorService executorService, boolean shutdownExecutor, final int connectionTimeout, final String protocol) {
        this.address = address;
        this.port = port;
        this.handler = handler;
        this.saslOptions = saslOptions;
        this.sslContext = sslContext;
        this.executorService = executorService;
        this.shutdownExecutor = shutdownExecutor;
        this.protocol = protocol;
        this.connectionTimeout = connectionTimeout > 0 ? connectionTimeout : DEFAULT_CONNECTION_TIMEOUT;
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
    public String getProtocol() {
        return protocol;
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
        return connectionTimeout;
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
        return new ClientConfigurationImpl(address.getHostAddress(), port, null, null, null, createDefaultExecutor(), true, null);
    }
    public static ModelControllerClientConfiguration create(final String protocol, final InetAddress address, final int port) {
        return new ClientConfigurationImpl(address.getHostAddress(), port, null, null, null, createDefaultExecutor(), true, protocol);
    }

    public static ModelControllerClientConfiguration create(final InetAddress address, final int port, final CallbackHandler handler){
        return new ClientConfigurationImpl(address.getHostAddress(), port, handler, null, null, createDefaultExecutor(), true, null);
    }

    public static ModelControllerClientConfiguration create(final String protocol,final InetAddress address, final int port, final CallbackHandler handler){
        return new ClientConfigurationImpl(address.getHostAddress(), port, handler, null, null, createDefaultExecutor(), true, protocol);
    }

    public static ModelControllerClientConfiguration create(final InetAddress address, final int port, final CallbackHandler handler, final Map<String, String> saslOptions){
        return new ClientConfigurationImpl(address.getHostAddress(), port, handler, saslOptions, null, createDefaultExecutor(), true, null);
    }

    public static ModelControllerClientConfiguration create(final String protocol, final InetAddress address, final int port, final CallbackHandler handler, final Map<String, String> saslOptions){
        return new ClientConfigurationImpl(address.getHostAddress(), port, handler, saslOptions, null, createDefaultExecutor(), true, protocol);
    }

    public static ModelControllerClientConfiguration create(final String hostName, final int port) throws UnknownHostException {
        return new ClientConfigurationImpl(hostName, port, null, null, null, createDefaultExecutor(), true, null);
    }

    public static ModelControllerClientConfiguration create(final String protocol, final String hostName, final int port) throws UnknownHostException {
        return new ClientConfigurationImpl(hostName, port, null, null, null, createDefaultExecutor(), true, protocol);
    }

    public static ModelControllerClientConfiguration create(final String hostName, final int port, final CallbackHandler handler) throws UnknownHostException {
        return new ClientConfigurationImpl(hostName, port, handler, null, null, createDefaultExecutor(), true, null);
    }

    public static ModelControllerClientConfiguration create(final String protocol, final String hostName, final int port, final CallbackHandler handler) throws UnknownHostException {
        return new ClientConfigurationImpl(hostName, port, handler, null, null, createDefaultExecutor(), true, protocol);
    }

    public static ModelControllerClientConfiguration create(final String hostName, final int port, final CallbackHandler handler, final SSLContext sslContext) throws UnknownHostException {
        return new ClientConfigurationImpl(hostName, port, handler, null, sslContext, createDefaultExecutor(), true, null);
    }

    public static ModelControllerClientConfiguration create(final String protocol, final String hostName, final int port, final CallbackHandler handler, final SSLContext sslContext) throws UnknownHostException {
        return new ClientConfigurationImpl(hostName, port, handler, null, sslContext, createDefaultExecutor(), true, protocol);
    }

    public static ModelControllerClientConfiguration create(final String hostName, final int port, final CallbackHandler handler, final SSLContext sslContext, final int connectionTimeout) throws UnknownHostException {
        return new ClientConfigurationImpl(hostName, port, handler, null, sslContext, createDefaultExecutor(), true, connectionTimeout, null);
    }

    public static ModelControllerClientConfiguration create(final String protocol, final String hostName, final int port, final CallbackHandler handler, final SSLContext sslContext, final int connectionTimeout) throws UnknownHostException {
        return new ClientConfigurationImpl(hostName, port, handler, null, sslContext, createDefaultExecutor(), true, connectionTimeout, protocol);
    }

    public static ModelControllerClientConfiguration create(final String protocol, final String hostName, final int port, final CallbackHandler handler, final SSLContext sslContext, final int connectionTimeout, final Map<String, String> saslOptions) throws UnknownHostException {
        return new ClientConfigurationImpl(hostName, port, handler, saslOptions, sslContext, createDefaultExecutor(), true, connectionTimeout, protocol);
    }

    public static ModelControllerClientConfiguration create(final String hostName, final int port, final CallbackHandler handler, final Map<String, String> saslOptions) throws UnknownHostException {
        return new ClientConfigurationImpl(hostName, port, handler, saslOptions, null, createDefaultExecutor(), true, null);
    }

    public static ModelControllerClientConfiguration create(final String protocol, final String hostName, final int port, final CallbackHandler handler, final Map<String, String> saslOptions) throws UnknownHostException {
        return new ClientConfigurationImpl(hostName, port, handler, saslOptions, null, createDefaultExecutor(), true, protocol);
    }

    private static int getSystemProperty(final String name, final int defaultValue) {
        final String value = getStringProperty(name);
        try {
            return value == null ? defaultValue : Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static String getStringProperty(final String name) {
        return getSecurityManager() == null ? getProperty(name) : doPrivileged(new PrivilegedAction<String>() {
            @Override
            public String run() {
                return getProperty(name);
            }
        });
    }
}
