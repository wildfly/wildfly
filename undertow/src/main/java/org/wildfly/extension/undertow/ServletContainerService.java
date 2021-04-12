/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

import io.undertow.connector.ByteBufferPool;
import io.undertow.security.api.AuthenticationMechanismFactory;
import io.undertow.server.handlers.cache.DirectBufferCache;
import io.undertow.servlet.api.CrawlerSessionManagerConfig;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletStackTraces;
import io.undertow.servlet.api.SessionPersistenceManager;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.xnio.XnioWorker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Central Undertow 'Container' HTTP listeners will make this container accessible whilst deployers will add content.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class ServletContainerService implements Service<ServletContainerService> {

    private final Consumer<ServletContainerService> serviceConsumer;
    private final Supplier<SessionPersistenceManager> sessionPersistenceManager;
    private final Supplier<DirectBufferCache> bufferCache;
    private final Supplier<ByteBufferPool> websocketsBufferPool;
    private final Supplier<XnioWorker> websocketsWorker;
    private final boolean allowNonStandardWrappers;
    private final ServletStackTraces stackTraces;
    private final SessionCookieConfig sessionCookieConfig;
    private final JSPConfig jspConfig;
    private final String defaultEncoding;
    private final boolean useListenerEncoding;
    private final boolean ignoreFlush;
    private final boolean eagerFilterInit;
    private final int defaultSessionTimeout;
    private final boolean disableCachingForSecuredPages;
    private final Boolean directoryListingEnabled;
    private final int sessionIdLength;
    private final CrawlerSessionManagerConfig crawlerSessionManagerConfig;
    private final boolean websocketsEnabled;
    private final boolean dispatchWebsocketInvocationToWorker;
    private final boolean perMessageDeflate;
    private final int deflaterLevel;
    private final Map<String, String> mimeMappings;
    private final List<String> welcomeFiles;
    private final boolean proactiveAuth;
    private final Map<String, AuthenticationMechanismFactory> authenticationMechanisms;
    private final Integer maxSessions;
    private final boolean disableFileWatchService;
    private final boolean disableSessionIdReuse;
    private final int fileCacheMetadataSize;
    private final int fileCacheMaxFileSize;
    private final Integer fileCacheTimeToLive;
    private final int defaultCookieVersion;
    private boolean preservePathOnForward;

    private volatile ServletContainer servletContainer;

    public ServletContainerService(
            final Consumer<ServletContainerService> serviceConsumer, final Supplier<SessionPersistenceManager> sessionPersistenceManager, final Supplier<DirectBufferCache> bufferCache,
            final Supplier<ByteBufferPool> websocketsBufferPool, final Supplier<XnioWorker> websocketsWorker,
            boolean allowNonStandardWrappers, ServletStackTraces stackTraces, SessionCookieConfig sessionCookieConfig, JSPConfig jspConfig,
            String defaultEncoding, boolean useListenerEncoding, boolean ignoreFlush, boolean eagerFilterInit, int defaultSessionTimeout,
            boolean disableCachingForSecuredPages, boolean websocketsEnabled, boolean dispatchWebsocketInvocationToWorker, boolean perMessageDeflate,
            int deflaterLevel, Map<String, String> mimeMappings, List<String> welcomeFiles, Boolean directoryListingEnabled, boolean proactiveAuth,
            int sessionIdLength, Map<String, AuthenticationMechanismFactory> authenticationMechanisms, Integer maxSessions,
            CrawlerSessionManagerConfig crawlerSessionManagerConfig, boolean disableFileWatchService, boolean disableSessionIdReuse, int fileCacheMetadataSize, int fileCacheMaxFileSize, Integer fileCacheTimeToLive, int defaultCookieVersion, boolean preservePathOnForward) {
        this.serviceConsumer = serviceConsumer;
        this.sessionPersistenceManager = sessionPersistenceManager;
        this.bufferCache = bufferCache;
        this.websocketsBufferPool = websocketsBufferPool;
        this.websocketsWorker = websocketsWorker;
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
        this.perMessageDeflate = perMessageDeflate;
        this.deflaterLevel = deflaterLevel;
        this.directoryListingEnabled = directoryListingEnabled;
        this.proactiveAuth = proactiveAuth;
        this.maxSessions = maxSessions;
        this.crawlerSessionManagerConfig = crawlerSessionManagerConfig;
        this.disableFileWatchService = disableFileWatchService;
        this.welcomeFiles = new ArrayList<>(welcomeFiles);
        this.mimeMappings = new HashMap<>(mimeMappings);
        this.sessionIdLength = sessionIdLength;
        this.authenticationMechanisms = authenticationMechanisms;
        this.disableSessionIdReuse = disableSessionIdReuse;
        this.fileCacheMetadataSize = fileCacheMetadataSize;
        this.fileCacheMaxFileSize = fileCacheMaxFileSize;
        this.fileCacheTimeToLive = fileCacheTimeToLive;
        this.defaultCookieVersion = defaultCookieVersion;
        this.preservePathOnForward = preservePathOnForward;
    }

    @Override
    public void start(final StartContext context) {
        servletContainer = ServletContainer.Factory.newInstance();
        serviceConsumer.accept(this);
    }

    @Override
    public void stop(final StopContext context) {
        serviceConsumer.accept(null);
    }

    @Override
    public ServletContainerService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public Map<String, AuthenticationMechanismFactory> getAuthenticationMechanisms() {
        return authenticationMechanisms;
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

    public DirectBufferCache getBufferCache() {
        return bufferCache != null ? bufferCache.get() : null;
    }

    public boolean isDisableCachingForSecuredPages() {
        return disableCachingForSecuredPages;
    }

    public boolean isDispatchWebsocketInvocationToWorker() {
        return dispatchWebsocketInvocationToWorker;
    }

    public boolean isPerMessageDeflate() {
        return perMessageDeflate;
    }

    public int getDeflaterLevel() {
        return deflaterLevel;
    }

    public boolean isWebsocketsEnabled() {
        return websocketsEnabled;
    }

    public boolean isDisableSessionIdReuse() {
        return disableSessionIdReuse;
    }

    public SessionPersistenceManager getSessionPersistenceManager() {
        return sessionPersistenceManager != null ? sessionPersistenceManager.get() : null;
    }

    public XnioWorker getWebsocketsWorker() {
        return websocketsWorker != null ? websocketsWorker.get() : null;
    }

    public ByteBufferPool getWebsocketsBufferPool() {
        return websocketsBufferPool != null ? websocketsBufferPool.get() : null;
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

    public Map<String, String> getMimeMappings() {
        return Collections.unmodifiableMap(mimeMappings);
    }

    public List<String> getWelcomeFiles() {
        return welcomeFiles;
    }

    public Boolean getDirectoryListingEnabled() {
        return directoryListingEnabled;
    }


    public boolean isProactiveAuth() {
        return proactiveAuth;
    }

    public int getSessionIdLength() {
        return sessionIdLength;
    }

    public Integer getMaxSessions() {
        return maxSessions;
    }

    public boolean isDisableFileWatchService() {
        return disableFileWatchService;
    }

    public CrawlerSessionManagerConfig getCrawlerSessionManagerConfig() {
        return crawlerSessionManagerConfig;
    }

    public int getFileCacheMetadataSize() {
        return fileCacheMetadataSize;
    }

    public int getFileCacheMaxFileSize() {
        return fileCacheMaxFileSize;
    }

    public Integer getFileCacheTimeToLive() {
        return fileCacheTimeToLive;
    }

    public int getDefaultCookieVersion() {
        return defaultCookieVersion;
    }

    public boolean isPreservePathOnForward() {
        return preservePathOnForward;
    }
}
