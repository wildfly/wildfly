/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import io.undertow.servlet.api.CrawlerSessionManagerConfig;
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
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;

import java.util.Collection;
import java.util.List;

/**
 * Global session cookie config
 *
 * @author Stuart Douglas
 */
class CrawlerSessionManagementDefinition extends PersistentResourceDefinition {
    static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.SETTING, Constants.CRAWLER_SESSION_MANAGEMENT);

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

    static final Collection<AttributeDefinition> ATTRIBUTES = List.of(USER_AGENTS, SESSION_TIMEOUT);

    CrawlerSessionManagementDefinition() {
        super(new SimpleResourceDefinition.Parameters(PATH_ELEMENT, UndertowExtension.getResolver(PATH_ELEMENT.getKeyValuePair()))
                .setAddHandler(new CrawlerSessionManagementAdd())
                .setRemoveHandler(new CrawlerSessionManagementRemove())
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    static CrawlerSessionManagerConfig getConfig(final ExpressionResolver context, final ModelNode model) throws OperationFailedException {
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

    private static class CrawlerSessionManagementRemove extends RestartParentResourceRemoveHandler {

        protected CrawlerSessionManagementRemove() {
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
