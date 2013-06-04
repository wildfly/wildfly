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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * Global session cookie config
 *
 * @author Stuart Douglas
 */
class SessionCookieDefinition extends PersistentResourceDefinition {

    private static final SessionCookieAdd ADD_OPERATION = new SessionCookieAdd();

    static final SessionCookieDefinition INSTANCE = new SessionCookieDefinition();


    protected static final SimpleAttributeDefinition NAME =
            new SimpleAttributeDefinitionBuilder(Constants.NAME, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition DOMAIN =
            new SimpleAttributeDefinitionBuilder(Constants.DOMAIN, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition COMMENT =
            new SimpleAttributeDefinitionBuilder(Constants.COMMENT, ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition HTTP_ONLY =
            new SimpleAttributeDefinitionBuilder(Constants.HTTP_ONLY, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition SECURE =
            new SimpleAttributeDefinitionBuilder(Constants.SECURE, ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition MAX_AGE =
            new SimpleAttributeDefinitionBuilder(Constants.MAX_AGE, ModelType.INT, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
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
                ADD_OPERATION,
                new ServiceRemoveStepHandler(SessionCookieConfigService.SERVICE_NAME, ADD_OPERATION));
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES_MAP.values();
    }

    private static class SessionCookieAdd extends AbstractAddStepHandler {
        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            for (AttributeDefinition def : ATTRIBUTES) {
                def.validateAndSet(operation, model);
            }
        }

        @Override
        protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
            final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
            final String servletContainerName = address.getElement(address.size() - 2).getValue();

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
            final SessionCookieConfigService service = new SessionCookieConfigService(name, domain, comment, httpOnly, secure, maxAge);
            ServiceBuilder<SessionCookieConfigService> builder = context.getServiceTarget().addService(SessionCookieConfigService.SERVICE_NAME.append(servletContainerName), service);
            if(verificationHandler != null) {
                builder.addListener(verificationHandler);
            }
            ServiceController<SessionCookieConfigService> controller = builder.install();
            if(newControllers != null) {
                newControllers.add(controller);
            }

        }
    }
}
