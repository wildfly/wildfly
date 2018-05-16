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

import static org.wildfly.extension.undertow.Capabilities.CAPABILITY_BYTE_BUFFER_POOL;
import static org.wildfly.extension.undertow.Capabilities.REF_IO_WORKER;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.RestartParentResourceAddHandler;
import org.jboss.as.controller.RestartParentResourceRemoveHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;

/**
 * Global websocket configuration
 *
 * @author Stuart Douglas
 */
class WebsocketsDefinition extends PersistentResourceDefinition {

    private static final RuntimeCapability<Void> WEBSOCKET_CAPABILITY = RuntimeCapability.Builder.of(Capabilities.CAPABILITY_WEBSOCKET, true, UndertowListener.class)
            .setDynamicNameMapper(pathElements -> new String[]{
                    pathElements.getParent().getLastElement().getValue()
            })
            .build();

    protected static final SimpleAttributeDefinition BUFFER_POOL =
            new SimpleAttributeDefinitionBuilder("buffer-pool", ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode("default"))
                    .setCapabilityReference(CAPABILITY_BYTE_BUFFER_POOL)
                    .build();

    protected static final SimpleAttributeDefinition WORKER =
            new SimpleAttributeDefinitionBuilder("worker", ModelType.STRING, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode("default"))
                    .setCapabilityReference(REF_IO_WORKER)
                    .build();

    protected static final SimpleAttributeDefinition DISPATCH_TO_WORKER =
            new SimpleAttributeDefinitionBuilder("dispatch-to-worker", ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(true))
                    .build();

    protected static final SimpleAttributeDefinition PER_MESSAGE_DEFLATE =
            new SimpleAttributeDefinitionBuilder("per-message-deflate", ModelType.BOOLEAN, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowExpression(true)
                    .setDefaultValue(new ModelNode(false))
                    .build();

    protected static final SimpleAttributeDefinition DEFLATER_LEVEL =
            new SimpleAttributeDefinitionBuilder("deflater-level", ModelType.INT, true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setAllowExpression(true)
                    .setValidator(new IntRangeValidator(0, 9, true, true))
                    .setDefaultValue(new ModelNode(0))
                    .build();

    protected static final List<AttributeDefinition> ATTRIBUTES = Arrays.asList(
            BUFFER_POOL,
            WORKER,
            DISPATCH_TO_WORKER,
            PER_MESSAGE_DEFLATE,
            DEFLATER_LEVEL
    );

    static final WebsocketsDefinition INSTANCE = new WebsocketsDefinition();


    private WebsocketsDefinition() {
        super(UndertowExtension.PATH_WEBSOCKETS,
                UndertowExtension.getResolver(UndertowExtension.PATH_WEBSOCKETS.getKeyValuePair()),
                new WebsocketsAdd(),
                new WebsocketsRemove());
    }

    @Override
    public void registerCapabilities(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerCapability(WEBSOCKET_CAPABILITY);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    public WebSocketInfo getConfig(final OperationContext context, final ModelNode model) throws OperationFailedException {
        if (!model.isDefined()) {
            return null;
        }
        boolean dispatchToWorker = DISPATCH_TO_WORKER.resolveModelAttribute(context, model).asBoolean();
        String bufferPool = BUFFER_POOL.resolveModelAttribute(context, model).asString();
        String worker = WORKER.resolveModelAttribute(context, model).asString();
        boolean perMessageDeflate = PER_MESSAGE_DEFLATE.resolveModelAttribute(context, model).asBoolean();
        int deflaterLevel = DEFLATER_LEVEL.resolveModelAttribute(context, model).asInt();

        return new WebSocketInfo(worker, bufferPool, dispatchToWorker, perMessageDeflate, deflaterLevel);
    }

    private static class WebsocketsAdd extends RestartParentResourceAddHandler {
        protected WebsocketsAdd() {
            super(ServletContainerDefinition.INSTANCE.getPathElement().getKey(), Collections.singleton(WEBSOCKET_CAPABILITY), ATTRIBUTES);
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
            return ServletContainerDefinition.SERVLET_CONTAINER_CAPABILITY.getCapabilityServiceName(parentAddress);
        }
    }

    private static class WebsocketsRemove extends RestartParentResourceRemoveHandler {

        protected WebsocketsRemove() {
            super(ServletContainerDefinition.INSTANCE.getPathElement().getKey());
        }

        @Override
        protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel) throws OperationFailedException {
            ServletContainerAdd.INSTANCE.installRuntimeServices(context, parentModel, parentAddress.getLastElement().getValue());
        }

        @Override
        protected ServiceName getParentServiceName(PathAddress parentAddress) {
            return ServletContainerDefinition.SERVLET_CONTAINER_CAPABILITY.getCapabilityServiceName(parentAddress);
        }
    }

    public static class WebSocketInfo {
        private final String worker;
        private final String bufferPool;
        private final boolean dispatchToWorker;
        private final boolean perMessageDeflate;
        private final int deflaterLevel;

        public WebSocketInfo(String worker, String bufferPool, boolean dispatchToWorker, boolean perMessageDeflate,
                int deflaterLevel) {
            this.worker = worker;
            this.bufferPool = bufferPool;
            this.dispatchToWorker = dispatchToWorker;
            this.perMessageDeflate = perMessageDeflate;
            this.deflaterLevel = deflaterLevel;
        }

        public String getWorker() {
            return worker;
        }

        public String getBufferPool() {
            return bufferPool;
        }

        public boolean isDispatchToWorker() {
            return dispatchToWorker;
        }

        public boolean isPerMessageDeflate() {
            return perMessageDeflate;
        }

        public int getDeflaterLevel() {
            return deflaterLevel;
        }
    }
}
