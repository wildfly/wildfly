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

import io.undertow.servlet.api.CrawlerSessionManagerConfig;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.RestartParentResourceAddHandler;
import org.jboss.as.controller.RestartParentResourceRemoveHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
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
class CrawlerSessionManagementDefinition extends PersistentResourceDefinition {

    static final CrawlerSessionManagementDefinition INSTANCE = new CrawlerSessionManagementDefinition();

    protected static final SimpleAttributeDefinition USER_AGENTS =
            new SimpleAttributeDefinitionBuilder(Constants.USER_AGENTS, ModelType.STRING, true)
                    .setRestartAllServices()
                    .setAllowExpression(true)
                    .build();
    protected static final SimpleAttributeDefinition SESSION_TIMEOUT =
            new SimpleAttributeDefinitionBuilder(Constants.SESSION_TIMEOUT, ModelType.INT, true)
                    .setRestartAllServices()
                    .setMeasurementUnit(MeasurementUnit.SECONDS)
                    .setAllowExpression(true)
                    .build();

    protected static final SimpleAttributeDefinition[] ATTRIBUTES = {
            USER_AGENTS,
            SESSION_TIMEOUT
    };
    static final Map<String, AttributeDefinition> ATTRIBUTES_MAP = new HashMap<>();

    static {
        for (SimpleAttributeDefinition attr : ATTRIBUTES) {
            ATTRIBUTES_MAP.put(attr.getName(), attr);
        }
    }


    private CrawlerSessionManagementDefinition() {
        super(UndertowExtension.CRAWLER_SESSION_MANAGEMENT,
                UndertowExtension.getResolver(UndertowExtension.CRAWLER_SESSION_MANAGEMENT.getKeyValuePair()),
                new CrawlerSessionManagementAdd(),
                new CrawlerSessionManagementRemove());
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES_MAP.values();
    }

    public CrawlerSessionManagerConfig getConfig(final OperationContext context, final ModelNode model) throws OperationFailedException {
        if(!model.isDefined()) {
            return null;
        }
        ModelNode agents = USER_AGENTS.resolveModelAttribute(context, model);
        ModelNode timeout = SESSION_TIMEOUT.resolveModelAttribute(context, model);
        if(timeout.isDefined() && agents.isDefined()) {
            return new CrawlerSessionManagerConfig(timeout.asInt(), agents.asString());
        } else if(timeout.isDefined()) {
            return new CrawlerSessionManagerConfig(timeout.asInt());
        } else if(agents.isDefined()) {
            return new CrawlerSessionManagerConfig(agents.asString());
        }
        return new CrawlerSessionManagerConfig();
    }


    private static class CrawlerSessionManagementAdd extends RestartParentResourceAddHandler {
        protected CrawlerSessionManagementAdd() {
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

    private static class CrawlerSessionManagementRemove extends RestartParentResourceRemoveHandler {

        protected CrawlerSessionManagementRemove() {
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
