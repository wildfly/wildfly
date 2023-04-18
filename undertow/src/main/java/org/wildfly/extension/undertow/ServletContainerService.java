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
}
