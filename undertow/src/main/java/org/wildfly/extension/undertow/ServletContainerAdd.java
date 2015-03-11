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
import io.undertow.servlet.api.ServletStackTraces;
import io.undertow.servlet.api.SessionPersistenceManager;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.io.IOServices;
import org.xnio.Pool;
import org.xnio.XnioWorker;

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
        final boolean persistentSessions = PersistentSessionsDefinition.isEnabled(context, fullModel.get(PersistentSessionsDefinition.INSTANCE.getPathElement().getKeyValuePair()));
        final boolean allowNonStandardWrappers = ServletContainerDefinition.ALLOW_NON_STANDARD_WRAPPERS.resolveModelAttribute(context, model).asBoolean();
        final String bufferCache = ServletContainerDefinition.DEFAULT_BUFFER_CACHE.resolveModelAttribute(context, model).asString();

        JSPConfig jspConfig = JspDefinition.INSTANCE.getConfig(context, fullModel.get(JspDefinition.INSTANCE.getPathElement().getKeyValuePair()));

        final String stackTracesString = ServletContainerDefinition.STACK_TRACE_ON_ERROR.resolveModelAttribute(context, model).asString();
        final ModelNode defaultEncodingValue = ServletContainerDefinition.DEFAULT_ENCODING.resolveModelAttribute(context, model);
        final String defaultEncoding = defaultEncodingValue.isDefined()? defaultEncodingValue.asString() : null;
        final boolean useListenerEncoding = ServletContainerDefinition.USE_LISTENER_ENCODING.resolveModelAttribute(context, model).asBoolean();
        final boolean ignoreFlush = ServletContainerDefinition.IGNORE_FLUSH.resolveModelAttribute(context, model).asBoolean();
        final boolean eagerFilterInit = ServletContainerDefinition.EAGER_FILTER_INIT.resolveModelAttribute(context, model).asBoolean();
        final boolean disableCachingForSecuredPages = ServletContainerDefinition.DISABLE_CACHING_FOR_SECURED_PAGES.resolveModelAttribute(context, model).asBoolean();

        final int sessionTimeout = ServletContainerDefinition.DEFAULT_SESSION_TIMEOUT.resolveModelAttribute(context, model).asInt();

        WebsocketsDefinition.WebSocketInfo info = WebsocketsDefinition.INSTANCE.getConfig(context, model);

        final ServletContainerService container = new ServletContainerService(allowNonStandardWrappers,
                ServletStackTraces.valueOf(stackTracesString.toUpperCase().replace('-', '_')),
                config,
                jspConfig,
                defaultEncoding,
                useListenerEncoding,
                ignoreFlush,
                eagerFilterInit,
                sessionTimeout,
                disableCachingForSecuredPages, info != null, info != null && info.isDispatchToWorker());
        final ServiceTarget target = context.getServiceTarget();
        final ServiceBuilder<ServletContainerService> builder = target.addService(UndertowService.SERVLET_CONTAINER.append(name), container);
        if(bufferCache != null) {
            builder.addDependency(BufferCacheService.SERVICE_NAME.append(bufferCache), DirectBufferCache.class, container.getBufferCacheInjectedValue());
        }
        if(persistentSessions) {
            builder.addDependency(AbstractPersistentSessionManager.SERVICE_NAME, SessionPersistenceManager.class, container.getSessionPersistenceManagerInjectedValue());
        }
        if(info != null) {
            builder.addDependency(IOServices.WORKER.append(info.getWorker()), XnioWorker.class, container.getWebsocketsWorker());
            builder.addDependency(IOServices.BUFFER_POOL.append(info.getBufferPool()), Pool.class, (InjectedValue)container.getWebsocketsBufferPool());
        }

        builder.setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install();
    }
}
