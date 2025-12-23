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
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceController;
import org.xnio.XnioWorker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class ServletContainerAdd extends AbstractBoottimeAddStepHandler {

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        installRuntimeServices(context.getCapabilityServiceTarget(), context, context.getCurrentAddress(), Resource.Tools.readModel(resource));
    }

    static void installRuntimeServices(CapabilityServiceTarget target, ExpressionResolver resolver, PathAddress address, ModelNode model) throws OperationFailedException {
        final CookieConfig sessionCookieConfig = SessionCookieDefinition.getConfig(resolver, model.get(SessionCookieDefinition.PATH_ELEMENT.getKeyValuePair()));
        final CookieConfig affinityCookieConfig = AffinityCookieDefinition.getConfig(resolver, model.get(AffinityCookieDefinition.PATH_ELEMENT.getKeyValuePair()));
        final CrawlerSessionManagerConfig crawlerSessionManagerConfig = CrawlerSessionManagementDefinition.getConfig(resolver, model.get(CrawlerSessionManagementDefinition.PATH_ELEMENT.getKeyValuePair()));
        final boolean persistentSessions = PersistentSessionsDefinition.isEnabled(model.get(PersistentSessionsDefinition.PATH_ELEMENT.getKeyValuePair()));
        final boolean allowNonStandardWrappers = ServletContainerDefinition.ALLOW_NON_STANDARD_WRAPPERS.resolveModelAttribute(resolver, model).asBoolean();
        final boolean proactiveAuth = ServletContainerDefinition.PROACTIVE_AUTHENTICATION.resolveModelAttribute(resolver, model).asBoolean();
        final String bufferCache = ServletContainerDefinition.DEFAULT_BUFFER_CACHE.resolveModelAttribute(resolver, model).asString();
        final boolean disableFileWatchService = ServletContainerDefinition.DISABLE_FILE_WATCH_SERVICE.resolveModelAttribute(resolver, model).asBoolean();
        final boolean disableSessionIdReususe = ServletContainerDefinition.DISABLE_SESSION_ID_REUSE.resolveModelAttribute(resolver, model).asBoolean();

        JSPConfig jspConfig = JspDefinition.getConfig(resolver, model.get(JspDefinition.PATH_ELEMENT.getKeyValuePair()));

        final String stackTracesString = ServletContainerDefinition.STACK_TRACE_ON_ERROR.resolveModelAttribute(resolver, model).asString();
        final ModelNode defaultEncodingValue = ServletContainerDefinition.DEFAULT_ENCODING.resolveModelAttribute(resolver, model);
        final String defaultEncoding = defaultEncodingValue.isDefined()? defaultEncodingValue.asString() : null;
        final boolean useListenerEncoding = ServletContainerDefinition.USE_LISTENER_ENCODING.resolveModelAttribute(resolver, model).asBoolean();
        final boolean ignoreFlush = ServletContainerDefinition.IGNORE_FLUSH.resolveModelAttribute(resolver, model).asBoolean();
        final boolean eagerFilterInit = ServletContainerDefinition.EAGER_FILTER_INIT.resolveModelAttribute(resolver, model).asBoolean();
        final boolean disableCachingForSecuredPages = ServletContainerDefinition.DISABLE_CACHING_FOR_SECURED_PAGES.resolveModelAttribute(resolver, model).asBoolean();
        final int sessionIdLength = ServletContainerDefinition.SESSION_ID_LENGTH.resolveModelAttribute(resolver, model).asInt();
        final int fileCacheMetadataSize = ServletContainerDefinition.FILE_CACHE_METADATA_SIZE.resolveModelAttribute(resolver, model).asInt();
        final int fileCacheMaxFileSize = ServletContainerDefinition.FILE_CACHE_MAX_FILE_SIZE.resolveModelAttribute(resolver, model).asInt();
        final ModelNode fileCacheTtlNode = ServletContainerDefinition.FILE_CACHE_TIME_TO_LIVE.resolveModelAttribute(resolver, model);
        final Integer fileCacheTimeToLive = fileCacheTtlNode.isDefined()  ? fileCacheTtlNode.asInt() : null;
        final int defaultCookieVersion = ServletContainerDefinition.DEFAULT_COOKIE_VERSION.resolveModelAttribute(resolver, model).asInt();
        final boolean preservePathOnForward = ServletContainerDefinition.PRESERVE_PATH_ON_FORWARD.resolveModelAttribute(resolver, model).asBoolean();
        boolean orphanSessionAllowed = ServletContainerDefinition.ORPHAN_SESSION_ALLOWED.resolveModelAttribute(resolver, model).asBoolean();

        Boolean directoryListingEnabled = ServletContainerDefinition.DIRECTORY_LISTING.resolveModelAttribute(resolver, model).asBooleanOrNull();
        Integer maxSessions = ServletContainerDefinition.MAX_SESSIONS.resolveModelAttribute(resolver, model).asIntOrNull();

        final int sessionTimeout = ServletContainerDefinition.DEFAULT_SESSION_TIMEOUT.resolveModelAttribute(resolver, model).asInt();

        final long defaultAsyncContextTimeout = ServletContainerDefinition.DEFAULT_ASYNC_CONTEXT_TIMEOUT.resolveModelAttribute(resolver, model).asLong();

        WebsocketsDefinition.WebSocketInfo webSocketInfo = WebsocketsDefinition.getConfig(resolver, model.get(WebsocketsDefinition.PATH_ELEMENT.getKeyValuePair()));

        Map<String, String> mimeMappings = resolveMimeMappings(resolver, model);

        List<String> welcomeFiles = resolveWelcomeFiles(model);

        final CapabilityServiceBuilder<?> builder = target.addCapability(ServletContainerDefinition.SERVLET_CONTAINER_CAPABILITY);
        final Supplier<SessionPersistenceManager> sessionPersistenceManager = persistentSessions ? builder.requires(AbstractPersistentSessionManager.SERVICE_NAME) : null;
        final Supplier<DirectBufferCache> directBufferCache = bufferCache != null ? builder.requires(BufferCacheService.SERVICE_NAME.append(bufferCache)) : null;
        final Supplier<ByteBufferPool> byteBufferPool = webSocketInfo != null ? builder.requiresCapability(Capabilities.CAPABILITY_BYTE_BUFFER_POOL, ByteBufferPool.class, webSocketInfo.getBufferPool()) : null;
        final Supplier<XnioWorker> xnioWorker = webSocketInfo != null ? builder.requiresCapability(Capabilities.REF_IO_WORKER, XnioWorker.class, webSocketInfo.getWorker()) : null;

        ServletStackTraces traces = ServletStackTraces.valueOf(stackTracesString.toUpperCase(Locale.ENGLISH).replace('-', '_'));
        ServletContainer container = ServletContainer.Factory.newInstance();
        ServletContainerService service = new ServletContainerService() {
            @Override
            public ServletContainer getServletContainer() {
                return container;
            }

            @Override
            public boolean isAllowNonStandardWrappers() {
                return allowNonStandardWrappers;
            }

            @Override
            public JSPConfig getJspConfig() {
                return jspConfig;
            }

            @Override
            public ServletStackTraces getStackTraces() {
                return traces;
            }

            @Override
            public CookieConfig getSessionCookieConfig() {
                return sessionCookieConfig;
            }

            @Override
            public CookieConfig getAffinityCookieConfig() {
                return affinityCookieConfig;
            }

            @Override
            public DirectBufferCache getBufferCache() {
                return (directBufferCache != null) ? directBufferCache.get() : null;
            }

            @Override
            public boolean isDisableCachingForSecuredPages() {
                return disableCachingForSecuredPages;
            }

            @Override
            public boolean isDispatchWebsocketInvocationToWorker() {
                return (webSocketInfo != null) && webSocketInfo.isDispatchToWorker();
            }

            @Override
            public boolean isPerMessageDeflate() {
                return (webSocketInfo != null) && webSocketInfo.isPerMessageDeflate();
            }

            @Override
            public int getDeflaterLevel() {
                return (webSocketInfo != null) ? webSocketInfo.getDeflaterLevel() : -1;
            }

            @Override
            public boolean isWebsocketsEnabled() {
                return webSocketInfo != null;
            }

            @Override
            public boolean isDisableSessionIdReuse() {
                return disableSessionIdReususe;
            }

            @Override
            public SessionPersistenceManager getSessionPersistenceManager() {
                return (sessionPersistenceManager != null) ? sessionPersistenceManager.get() : null;
            }

            @Override
            public XnioWorker getWebsocketsWorker() {
                return (xnioWorker != null) ? xnioWorker.get() : null;
            }

            @Override
            public ByteBufferPool getWebsocketsBufferPool() {
                return (byteBufferPool != null) ? byteBufferPool.get() : null;
            }

            @Override
            public String getDefaultEncoding() {
                return defaultEncoding;
            }

            @Override
            public boolean isUseListenerEncoding() {
                return useListenerEncoding;
            }

            @Override
            public boolean isIgnoreFlush() {
                return ignoreFlush;
            }

            @Override
            public boolean isEagerFilterInit() {
                return eagerFilterInit;
            }

            @Override
            public int getDefaultSessionTimeout() {
                return sessionTimeout;
            }

            @Override
            public Map<String, String> getMimeMappings() {
                return mimeMappings;
            }

            @Override
            public List<String> getWelcomeFiles() {
                return welcomeFiles;
            }

            @Override
            public Boolean getDirectoryListingEnabled() {
                return directoryListingEnabled;
            }

            @Override
            public boolean isProactiveAuth() {
                return proactiveAuth;
            }

            @Override
            public int getSessionIdLength() {
                return sessionIdLength;
            }

            @Override
            public Integer getMaxSessions() {
                return maxSessions;
            }

            @Override
            public boolean isDisableFileWatchService() {
                return disableFileWatchService;
            }

            @Override
            public CrawlerSessionManagerConfig getCrawlerSessionManagerConfig() {
                return crawlerSessionManagerConfig;
            }

            @Override
            public int getFileCacheMetadataSize() {
                return fileCacheMetadataSize;
            }

            @Override
            public int getFileCacheMaxFileSize() {
                return fileCacheMaxFileSize;
            }

            @Override
            public Integer getFileCacheTimeToLive() {
                return fileCacheTimeToLive;
            }

            @Override
            public int getDefaultCookieVersion() {
                return defaultCookieVersion;
            }

            @Override
            public boolean isPreservePathOnForward() {
                return preservePathOnForward;
            }

            @Override
            public boolean isOrphanSessionAllowed() {
                return orphanSessionAllowed;
            }

            @Override
            public long getDefaultAsyncContextTimeout() {
               return defaultAsyncContextTimeout;
            }
        };
        builder.setInstance(Service.newInstance(builder.provides(ServletContainerDefinition.SERVLET_CONTAINER_CAPABILITY), service));
        builder.setInitialMode(ServiceController.Mode.ON_DEMAND);
        builder.install();
    }

    private static Map<String, String> resolveMimeMappings(ExpressionResolver resolver, ModelNode model) throws OperationFailedException {
        if (!model.hasDefined(Constants.MIME_MAPPING)) return Map.of();
        List<Property> properties = model.get(Constants.MIME_MAPPING).asPropertyList();
        if (properties.size() == 1) {
            Map.Entry<String, String> entry = resolveMimeMapping(resolver, properties.get(0));
            return Map.of(entry.getKey(), entry.getValue());
        }
        Map<String, String> result = new HashMap<>();
        for (Property property : properties) {
            Map.Entry<String, String> entry = resolveMimeMapping(resolver, property);
            result.put(entry.getKey(), entry.getValue());
        }
        return Collections.unmodifiableMap(result);
    }

    private static Map.Entry<String, String> resolveMimeMapping(ExpressionResolver resolver, Property property) throws OperationFailedException {
        return Map.entry(property.getName(), MimeMappingDefinition.VALUE.resolveModelAttribute(resolver, property.getValue()).asString());
    }

    private static List<String> resolveWelcomeFiles(ModelNode model) {
        if (!model.hasDefined(Constants.WELCOME_FILE)) return List.of();
        List<Property> properties = model.get(Constants.WELCOME_FILE).asPropertyList();
        if (properties.size() == 1) {
            return List.of(properties.get(0).getName());
        }
        List<String> result = new ArrayList<>(properties.size());
        for (Property property : properties) {
            result.add(property.getName());
        }
        return Collections.unmodifiableList(result);
    }
}
