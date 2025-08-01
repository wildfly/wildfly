/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import io.undertow.connector.ByteBufferPool;
import io.undertow.server.handlers.cache.DirectBufferCache;
import io.undertow.servlet.api.CrawlerSessionManagerConfig;
import io.undertow.servlet.api.ServletContainer;
import io.undertow.servlet.api.ServletStackTraces;
import io.undertow.servlet.api.SessionPersistenceManager;

import org.xnio.XnioWorker;

import java.util.List;
import java.util.Map;

/**
 * Central Undertow 'Container' HTTP listeners will make this container accessible whilst deployers will add content.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public interface ServletContainerService {

    ServletContainer getServletContainer();

    boolean isAllowNonStandardWrappers();

    JSPConfig getJspConfig();

    ServletStackTraces getStackTraces();

    CookieConfig getSessionCookieConfig();

    CookieConfig getAffinityCookieConfig();

    DirectBufferCache getBufferCache();

    boolean isDisableCachingForSecuredPages();

    boolean isDispatchWebsocketInvocationToWorker();

    boolean isPerMessageDeflate();

    int getDeflaterLevel();

    boolean isWebsocketsEnabled();

    boolean isDisableSessionIdReuse();

    SessionPersistenceManager getSessionPersistenceManager();

    XnioWorker getWebsocketsWorker();

    ByteBufferPool getWebsocketsBufferPool();

    String getDefaultEncoding();

    boolean isUseListenerEncoding();

    boolean isIgnoreFlush();

    boolean isEagerFilterInit();

    int getDefaultSessionTimeout();

    Map<String, String> getMimeMappings();

    List<String> getWelcomeFiles();

    Boolean getDirectoryListingEnabled();

    boolean isProactiveAuth();

    int getSessionIdLength();

    Integer getMaxSessions();

    boolean isDisableFileWatchService();

    CrawlerSessionManagerConfig getCrawlerSessionManagerConfig();

    int getFileCacheMetadataSize();

    int getFileCacheMaxFileSize();

    Integer getFileCacheTimeToLive();

    int getDefaultCookieVersion();

    boolean isPreservePathOnForward();

    boolean isOrphanSessionAllowed();

    long getDefaultAsyncContextTimeout();
}
