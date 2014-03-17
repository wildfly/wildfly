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
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
final class ServletContainerAdd extends AbstractBoottimeAddStepHandler {
    static final ServletContainerAdd INSTANCE = new ServletContainerAdd();

    ServletContainerAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition def : ServletContainerDefinition.INSTANCE.getAttributes()) {
            def.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, final ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();

        installRuntimeServices(context, model, newControllers, name);

    }

    public void installRuntimeServices(OperationContext context, ModelNode model, List<ServiceController<?>> newControllers, String name) throws OperationFailedException {
        final ModelNode fullModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));

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

        final ServletContainerService container = new ServletContainerService(allowNonStandardWrappers,
                ServletStackTraces.valueOf(stackTracesString.toUpperCase().replace('-', '_')),
                config,
                jspConfig,
                defaultEncoding,
                useListenerEncoding,
                ignoreFlush,
                eagerFilterInit);
        final ServiceTarget target = context.getServiceTarget();
        final ServiceBuilder<ServletContainerService> builder = target.addService(UndertowService.SERVLET_CONTAINER.append(name), container);
        if(bufferCache != null) {
            builder.addDependency(BufferCacheService.SERVICE_NAME.append(bufferCache), DirectBufferCache.class, container.getBufferCacheInjectedValue());
        }
        if(persistentSessions) {
            builder.addDependency(AbstractPersistentSessionManager.SERVICE_NAME, SessionPersistenceManager.class, container.getSessionPersistenceManagerInjectedValue());
        }

        newControllers.add(builder
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install());
    }
}
