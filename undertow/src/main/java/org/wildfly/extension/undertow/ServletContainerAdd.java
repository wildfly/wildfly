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

import io.undertow.security.api.AuthenticationMechanismFactory;
import io.undertow.server.handlers.cache.DirectBufferCache;
import io.undertow.servlet.api.CrawlerSessionManagerConfig;
import io.undertow.servlet.api.ServletStackTraces;
import io.undertow.servlet.api.SessionPersistenceManager;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.value.InjectedValue;
import org.jboss.security.negotiation.NegotiationMechanismFactory;
import org.wildfly.extension.undertow.security.digest.DigestAuthenticationMechanismFactory;
import org.xnio.Pool;
import org.xnio.XnioWorker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
final class ServletContainerAdd extends AbstractBoottimeAddStepHandler {
    static final ServletContainerAdd INSTANCE = new ServletContainerAdd();

    ServletContainerAdd() {
        super(ServletContainerDefinition.ATTRIBUTES);
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        installRuntimeServices(context, resource.getModel(), context.getCurrentAddressValue());
    }

    public void installRuntimeServices(OperationContext context, ModelNode model, String name) throws OperationFailedException {
        final ModelNode fullModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS), 2);

        final SessionCookieConfig config = SessionCookieDefinition.INSTANCE.getConfig(context, fullModel.get(SessionCookieDefinition.INSTANCE.getPathElement().getKeyValuePair()));
        final CrawlerSessionManagerConfig crawlerSessionManagerConfig = CrawlerSessionManagementDefinition.INSTANCE.getConfig(context, fullModel.get(CrawlerSessionManagementDefinition.INSTANCE.getPathElement().getKeyValuePair()));
        final boolean persistentSessions = PersistentSessionsDefinition.isEnabled(context, fullModel.get(PersistentSessionsDefinition.INSTANCE.getPathElement().getKeyValuePair()));
        final boolean allowNonStandardWrappers = ServletContainerDefinition.ALLOW_NON_STANDARD_WRAPPERS.resolveModelAttribute(context, model).asBoolean();
        final boolean proactiveAuth = ServletContainerDefinition.PROACTIVE_AUTHENTICATION.resolveModelAttribute(context, model).asBoolean();
        final String bufferCache = ServletContainerDefinition.DEFAULT_BUFFER_CACHE.resolveModelAttribute(context, model).asString();
        final boolean disableFileWatchService = ServletContainerDefinition.DISABLE_FILE_WATCH_SERVICE.resolveModelAttribute(context, model).asBoolean();
        final boolean disableSessionIdReususe = ServletContainerDefinition.DISABLE_SESSION_ID_REUSE.resolveModelAttribute(context, model).asBoolean();

        JSPConfig jspConfig = JspDefinition.INSTANCE.getConfig(context, fullModel.get(JspDefinition.INSTANCE.getPathElement().getKeyValuePair()));

        final String stackTracesString = ServletContainerDefinition.STACK_TRACE_ON_ERROR.resolveModelAttribute(context, model).asString();
        final ModelNode defaultEncodingValue = ServletContainerDefinition.DEFAULT_ENCODING.resolveModelAttribute(context, model);
        final String defaultEncoding = defaultEncodingValue.isDefined()? defaultEncodingValue.asString() : null;
        final boolean useListenerEncoding = ServletContainerDefinition.USE_LISTENER_ENCODING.resolveModelAttribute(context, model).asBoolean();
        final boolean ignoreFlush = ServletContainerDefinition.IGNORE_FLUSH.resolveModelAttribute(context, model).asBoolean();
        final boolean eagerFilterInit = ServletContainerDefinition.EAGER_FILTER_INIT.resolveModelAttribute(context, model).asBoolean();
        final boolean disableCachingForSecuredPages = ServletContainerDefinition.DISABLE_CACHING_FOR_SECURED_PAGES.resolveModelAttribute(context, model).asBoolean();
        final int sessionIdLength = ServletContainerDefinition.SESSION_ID_LENGTH.resolveModelAttribute(context, model).asInt();

        Boolean directoryListingEnabled = null;
        if(model.hasDefined(Constants.DIRECTORY_LISTING)) {
            directoryListingEnabled = ServletContainerDefinition.DIRECTORY_LISTING.resolveModelAttribute(context, model).asBoolean();
        }
        Integer maxSessions = null;
        if(model.hasDefined(Constants.MAX_SESSIONS)) {
            maxSessions = ServletContainerDefinition.MAX_SESSIONS.resolveModelAttribute(context, model).asInt();
        }

        final int sessionTimeout = ServletContainerDefinition.DEFAULT_SESSION_TIMEOUT.resolveModelAttribute(context, model).asInt();

        WebsocketsDefinition.WebSocketInfo webSocketInfo = WebsocketsDefinition.INSTANCE.getConfig(context, fullModel.get(WebsocketsDefinition.INSTANCE.getPathElement().getKeyValuePair()));

        final Map<String, String> mimeMappings = new HashMap<>();
        if (fullModel.hasDefined(Constants.MIME_MAPPING)) {
            for (final Property mapping : fullModel.get(Constants.MIME_MAPPING).asPropertyList()) {
                mimeMappings.put(mapping.getName(), MimeMappingDefinition.VALUE.resolveModelAttribute(context, mapping.getValue()).asString());
            }
        }

        List<String> welcomeFiles = new ArrayList<>();
        if (fullModel.hasDefined(Constants.WELCOME_FILE)) {
            for (final Property welcome : fullModel.get(Constants.WELCOME_FILE).asPropertyList()) {
                welcomeFiles.add(welcome.getName());
            }
        }

        // WFLY-2553 Adding default WildFly specific mechanisms here - subsequently we could enhance the servlet-container
        // config to override / add mechanisms.
        Map<String, AuthenticationMechanismFactory> authenticationMechanisms = new HashMap<>();
        authenticationMechanisms.put("SPNEGO", new NegotiationMechanismFactory());
        authenticationMechanisms.put(HttpServletRequest.DIGEST_AUTH, DigestAuthenticationMechanismFactory.FACTORY);

        final ServletContainerService container = new ServletContainerService(allowNonStandardWrappers,
                ServletStackTraces.valueOf(stackTracesString.toUpperCase().replace('-', '_')),
                config,
                jspConfig,
                defaultEncoding,
                useListenerEncoding,
                ignoreFlush,
                eagerFilterInit,
                sessionTimeout,
                disableCachingForSecuredPages, webSocketInfo != null, webSocketInfo != null && webSocketInfo.isDispatchToWorker(),
                webSocketInfo != null && webSocketInfo.isPerMessageDeflate(), webSocketInfo == null ? -1 : webSocketInfo.getDeflaterLevel(),
                mimeMappings,
                welcomeFiles, directoryListingEnabled, proactiveAuth, sessionIdLength, authenticationMechanisms, maxSessions, crawlerSessionManagerConfig, disableFileWatchService, disableSessionIdReususe);


        final CapabilityServiceBuilder<ServletContainerService> builder = context.getCapabilityServiceTarget()
                .addCapability(ServletContainerDefinition.SERVLET_CONTAINER_CAPABILITY, container);
        if(bufferCache != null) {
            builder.addDependency(BufferCacheService.SERVICE_NAME.append(bufferCache), DirectBufferCache.class, container.getBufferCacheInjectedValue());
        }
        if(persistentSessions) {
            builder.addDependency(AbstractPersistentSessionManager.SERVICE_NAME, SessionPersistenceManager.class, container.getSessionPersistenceManagerInjectedValue());
        }
        if(webSocketInfo != null) {
            builder.addCapabilityRequirement(Capabilities.REF_IO_WORKER, XnioWorker.class, container.getWebsocketsWorker(), webSocketInfo.getWorker());
            builder.addCapabilityRequirement(Capabilities.REF_BUFFER_POOL, Pool.class, (InjectedValue) container.getWebsocketsBufferPool(), webSocketInfo.getBufferPool());
        }

        builder.setInitialMode(ServiceController.Mode.ON_DEMAND)
               .addAliases(UndertowService.SERVLET_CONTAINER.append(name))
                .install();
    }
}
