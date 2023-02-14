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

import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.RestartParentResourceAddHandler;
import org.jboss.as.controller.RestartParentResourceRemoveHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;

/**
 * Resource definition for a Cookie configuration.
 *
 * @author Stuart Douglas
 * @author Radoslav Husar
 */
class CookieDefinition extends PersistentResourceDefinition {
    public static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.SETTING, Constants.SESSION_COOKIE);
    public static final PathElement AFFINITY_PATH_ELEMENT = PathElement.pathElement(Constants.SETTING, Constants.AFFINITY_COOKIE);

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
                    .setDeprecated(UndertowModel.VERSION_13_0_0.getVersion())
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


    static final Collection<AttributeDefinition> ATTRIBUTES = List.of(
            NAME,
            DOMAIN,
            COMMENT,
            HTTP_ONLY,
            SECURE,
            MAX_AGE);

    CookieDefinition(PathElement path) {
        super(new SimpleResourceDefinition.Parameters(path, UndertowExtension.getResolver(path.getKeyValuePair()))
                .setAddHandler(new SessionCookieAdd())
                .setRemoveHandler(new SessionCookieRemove())
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    static CookieConfig getConfig(final ExpressionResolver context, final ModelNode model) throws OperationFailedException {
        if (!model.isDefined()) {
            return null;
        }
        ModelNode nameValue = NAME.resolveModelAttribute(context, model);
        ModelNode domainValue = DOMAIN.resolveModelAttribute(context, model);
        ModelNode secureValue = SECURE.resolveModelAttribute(context, model);
        ModelNode httpOnlyValue = HTTP_ONLY.resolveModelAttribute(context, model);
        ModelNode maxAgeValue = MAX_AGE.resolveModelAttribute(context, model);

        final String name = nameValue.isDefined() ? nameValue.asString() : null;
        final String domain = domainValue.isDefined() ? domainValue.asString() : null;
        final Boolean secure = secureValue.isDefined() ? secureValue.asBoolean() : null;
        final Boolean httpOnly = httpOnlyValue.isDefined() ? httpOnlyValue.asBoolean() : null;
        final Integer maxAge = maxAgeValue.isDefined() ? maxAgeValue.asInt() : null;

        return new CookieConfig(name, domain, httpOnly, secure, maxAge);
    }


    private static class SessionCookieAdd extends RestartParentResourceAddHandler {
        protected SessionCookieAdd() {
            super(ServletContainerDefinition.PATH_ELEMENT.getKey());
        }

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            for (AttributeDefinition def : ATTRIBUTES) {
                def.validateAndSet(operation, model);
            }
        }

        @Override
        protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel) throws OperationFailedException {
            ServletContainerAdd.installRuntimeServices(context.getCapabilityServiceTarget(), context, parentAddress, parentModel);
        }

        @Override
        protected ServiceName getParentServiceName(PathAddress parentAddress) {
            return ServletContainerDefinition.SERVLET_CONTAINER_CAPABILITY.getCapabilityServiceName(parentAddress);
        }
    }

    private static class SessionCookieRemove extends RestartParentResourceRemoveHandler {

        protected SessionCookieRemove() {
            super(ServletContainerDefinition.PATH_ELEMENT.getKey());
        }

        @Override
        protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel) throws OperationFailedException {
            ServletContainerAdd.installRuntimeServices(context.getCapabilityServiceTarget(), context, parentAddress, parentModel);
        }

        @Override
        protected ServiceName getParentServiceName(PathAddress parentAddress) {
            return ServletContainerDefinition.SERVLET_CONTAINER_CAPABILITY.getCapabilityServiceName(parentAddress);
        }
    }
}
