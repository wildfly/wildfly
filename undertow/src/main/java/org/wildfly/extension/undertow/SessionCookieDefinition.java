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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.RestartParentResourceAddHandler;
import org.jboss.as.controller.RestartParentResourceRemoveHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Global session cookie config
 *
 * @author Stuart Douglas
 */
class SessionCookieDefinition extends PersistentResourceDefinition {

    static final SessionCookieDefinition INSTANCE = new SessionCookieDefinition();

    protected static final SimpleAttributeDefinition NAME =
            new SimpleAttributeDefinitionBuilder(Constants.NAME, ModelType.STRING, true)
                    .setRestartAllServices()
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition DOMAIN =
            new SimpleAttributeDefinitionBuilder(Constants.DOMAIN, ModelType.STRING, true)
                    .setRestartAllServices()
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition COMMENT =
            new SimpleAttributeDefinitionBuilder(Constants.COMMENT, ModelType.STRING, true)
                    .setRestartAllServices()
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition HTTP_ONLY =
            new SimpleAttributeDefinitionBuilder(Constants.HTTP_ONLY, ModelType.BOOLEAN, true)
                    .setRestartAllServices()
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition SECURE =
            new SimpleAttributeDefinitionBuilder(Constants.SECURE, ModelType.BOOLEAN, true)
                    .setRestartAllServices()
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition MAX_AGE =
            new SimpleAttributeDefinitionBuilder(Constants.MAX_AGE, ModelType.INT, true)
                    .setRestartAllServices()
                    .setAllowExpression(true)
                    .build();


    protected static final SimpleAttributeDefinition[] ATTRIBUTES = {
            NAME,
            DOMAIN,
            COMMENT,
            HTTP_ONLY,
            SECURE,
            MAX_AGE
    };
    static final Map<String, AttributeDefinition> ATTRIBUTES_MAP = new HashMap<>();

    static {
        for (SimpleAttributeDefinition attr : ATTRIBUTES) {
            ATTRIBUTES_MAP.put(attr.getName(), attr);
        }
    }


    private SessionCookieDefinition() {
        super(UndertowExtension.PATH_SESSION_COOKIE,
                UndertowExtension.getResolver(UndertowExtension.PATH_SESSION_COOKIE.getKeyValuePair()),
                new SessionCookieAdd(),
                new SessionCookieRemove());
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES_MAP.values();
    }

    public SessionCookieConfig getConfig(final OperationContext context, final ModelNode model) throws OperationFailedException {
        if(!model.isDefined()) {
            return null;
        }
        ModelNode nameValue = NAME.resolveModelAttribute(context, model);
        ModelNode domainValue = DOMAIN.resolveModelAttribute(context, model);
        ModelNode commentValue = COMMENT.resolveModelAttribute(context, model);
        ModelNode secureValue = SECURE.resolveModelAttribute(context, model);
        ModelNode httpOnlyValue = HTTP_ONLY.resolveModelAttribute(context, model);
        ModelNode maxAgeValue = MAX_AGE.resolveModelAttribute(context, model);
        final String name = nameValue.isDefined() ? nameValue.asString() : null;
        final String domain = domainValue.isDefined() ? domainValue.asString() : null;
        final String comment = commentValue.isDefined() ? commentValue.asString() : null;
        final Boolean secure = secureValue.isDefined() ? secureValue.asBoolean() : null;
        final Boolean httpOnly = httpOnlyValue.isDefined() ? httpOnlyValue.asBoolean() : null;
        final Integer maxAge = maxAgeValue.isDefined() ? maxAgeValue.asInt() : null;
        return new SessionCookieConfig(name, domain, comment, httpOnly, secure, maxAge);
    }


    private static class SessionCookieAdd extends RestartParentResourceAddHandler {
        protected SessionCookieAdd() {
            super(ServletContainerDefinition.INSTANCE.getPathElement().getKey());
        }

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            for (AttributeDefinition def : ATTRIBUTES) {
                def.validateAndSet(operation, model);
            }
        }

        @Override
        protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel) throws OperationFailedException {
            ServletContainerAdd.INSTANCE.installRuntimeServices(context, parentModel, parentAddress.getLastElement().getValue());
        }

        @Override
        protected ServiceName getParentServiceName(PathAddress parentAddress) {
            return UndertowService.SERVLET_CONTAINER.append(parentAddress.getLastElement().getValue());
        }
    }

    private static class SessionCookieRemove extends RestartParentResourceRemoveHandler {

        protected SessionCookieRemove() {
            super(ServletContainerDefinition.INSTANCE.getPathElement().getKey());
        }

        @Override
        protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel) throws OperationFailedException {
            ServletContainerAdd.INSTANCE.installRuntimeServices(context, parentModel, parentAddress.getLastElement().getValue());
        }

        @Override
        protected ServiceName getParentServiceName(PathAddress parentAddress) {
            return UndertowService.SERVLET_CONTAINER.append(parentAddress.getLastElement().getValue());
        }
    }
}
