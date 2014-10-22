/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import io.undertow.server.handlers.cache.DirectBufferCache;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletStackTraces;
import io.undertow.servlet.api.SessionPersistenceManager;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.xnio.Pool;
import org.xnio.XnioWorker;

import java.nio.ByteBuffer;

/**
 * Central Undertow 'Container' HTTP listeners will make this container accessible whilst deployers will add content.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ServletContainerService implements Service<ServletContainerService> {

    private final boolean allowNonStandardWrappers;
    private final ServletStackTraces stackTraces;
    private final SessionCookieConfig sessionCookieConfig;
    private final JSPConfig jspConfig;
    private volatile ServletContainer servletContainer;
    private final InjectedValue<DirectBufferCache> bufferCacheInjectedValue = new InjectedValue<>();
    private final InjectedValue<SessionPersistenceManager> sessionPersistenceManagerInjectedValue = new InjectedValue<>();
    private final String defaultEncoding;
    private final boolean useListenerEncoding;
    private final boolean ignoreFlush;
    private final boolean eagerFilterInit;
    private final int defaultSessionTimeout;
    private final boolean disableCachingForSecuredPages;

    private final boolean websocketsEnabled;
    private final InjectedValue<Pool<ByteBuffer>> websocketsBufferPool = new InjectedValue<>();
    private final InjectedValue<XnioWorker> websocketsWorker = new InjectedValue<>();
    private final boolean dispatchWebsocketInvocationToWorker;

    public ServletContainerService(boolean allowNonStandardWrappers, ServletStackTraces stackTraces, SessionCookieConfig sessionCookieConfig, JSPConfig jspConfig,
                                   String defaultEncoding, boolean useListenerEncoding, boolean ignoreFlush, boolean eagerFilterInit, int defaultSessionTimeout,
                                   boolean disableCachingForSecuredPages, boolean websocketsEnabled, boolean dispatchWebsocketInvocationToWorker) {
        this.allowNonStandardWrappers = allowNonStandardWrappers;
        this.stackTraces = stackTraces;
        this.sessionCookieConfig = sessionCookieConfig;
        this.jspConfig = jspConfig;
        this.defaultEncoding = defaultEncoding;
        this.useListenerEncoding = useListenerEncoding;
        this.ignoreFlush = ignoreFlush;
        this.eagerFilterInit = eagerFilterInit;
        this.defaultSessionTimeout = defaultSessionTimeout;
        this.disableCachingForSecuredPages = disableCachingForSecuredPages;
        this.websocketsEnabled = websocketsEnabled;
        this.dispatchWebsocketInvocationToWorker = dispatchWebsocketInvocationToWorker;
    }

    public void start(StartContext context) throws StartException {
        servletContainer = ServletContainer.Factory.newInstance();
    }

    public void stop(StopContext context) {

    }

    public ServletContainerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public ServletContainer getServletContainer() {
        return servletContainer;
    }

    public boolean isAllowNonStandardWrappers() {
        return allowNonStandardWrappers;
    }

    public JSPConfig getJspConfig() {
        return jspConfig;
    }

    public ServletStackTraces getStackTraces() {
        return stackTraces;
    }

    public SessionCookieConfig getSessionCookieConfig() {
        return sessionCookieConfig;
    }

    InjectedValue<DirectBufferCache> getBufferCacheInjectedValue() {
        return bufferCacheInjectedValue;
    }

    public DirectBufferCache getBufferCache() {
        return bufferCacheInjectedValue.getOptionalValue();
    }

    public boolean isDisableCachingForSecuredPages() {
        return disableCachingForSecuredPages;
    }

    public boolean isDispatchWebsocketInvocationToWorker() {
        return dispatchWebsocketInvocationToWorker;
    }

    public InjectedValue<XnioWorker> getWebsocketsWorker() {
        return websocketsWorker;
    }

    public InjectedValue<Pool<ByteBuffer>> getWebsocketsBufferPool() {
        return websocketsBufferPool;
    }

    public boolean isWebsocketsEnabled() {
        return websocketsEnabled;
    }

    InjectedValue<SessionPersistenceManager> getSessionPersistenceManagerInjectedValue() {
        return sessionPersistenceManagerInjectedValue;
    }

    public SessionPersistenceManager getSessionPersistenceManager() {
        return sessionPersistenceManagerInjectedValue.getOptionalValue();
    }

    public String getDefaultEncoding() {
        return defaultEncoding;
    }

    public boolean isUseListenerEncoding() {
        return useListenerEncoding;
    }

    public boolean isIgnoreFlush() {
        return ignoreFlush;
    }

    public boolean isEagerFilterInit() {
        return eagerFilterInit;
    }

    public int getDefaultSessionTimeout() {
        return defaultSessionTimeout;
    }
}
