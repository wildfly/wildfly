/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.BINDINGS_DIRECTORY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.JOURNAL_DIRECTORY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.LARGE_MESSAGES_DIRECTORY;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.PAGING_DIRECTORY;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathResourceDefinition;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2014 Red Hat inc.
 */
public class PathDefinition extends PersistentResourceDefinition {

    static final String DEFAULT_RELATIVE_TO = ServerEnvironment.SERVER_DATA_DIR;

    // base attribute for the 4 messaging path subresources.
    // each one define a different default values. Their respective attributes are accessed through the PATHS map.
    private static final SimpleAttributeDefinition PATH_BASE = create(PathResourceDefinition.PATH)
            .setAllowExpression(true)
            .setRequired(false)
            .setRestartAllServices()
            .build();

    public static final SimpleAttributeDefinition RELATIVE_TO = create(PathResourceDefinition.RELATIVE_TO)
            .setDefaultValue(new ModelNode(DEFAULT_RELATIVE_TO))
            .setRequired(false)
            .setRestartAllServices()
            .build();

    protected static final Map<String, SimpleAttributeDefinition> PATHS = new HashMap<String, SimpleAttributeDefinition>();

    private static final String DEFAULT_PATH = "activemq";
    static final String DEFAULT_BINDINGS_DIR = DEFAULT_PATH + File.separator + "bindings";
    static final String DEFAULT_JOURNAL_DIR = DEFAULT_PATH + File.separator + "journal";
    static final String DEFAULT_LARGE_MESSAGE_DIR = DEFAULT_PATH + File.separator + "largemessages";
    static final String DEFAULT_PAGING_DIR = DEFAULT_PATH + File.separator + "paging";

    static {
        PATHS.put(BINDINGS_DIRECTORY, create(PATH_BASE).setDefaultValue(new ModelNode(DEFAULT_BINDINGS_DIR)).build());
        PATHS.put(JOURNAL_DIRECTORY, create(PATH_BASE).setDefaultValue(new ModelNode(DEFAULT_JOURNAL_DIR)).build());
        PATHS.put(LARGE_MESSAGES_DIRECTORY, create(PATH_BASE).setDefaultValue(new ModelNode(DEFAULT_LARGE_MESSAGE_DIR)).build());
        PATHS.put(PAGING_DIRECTORY, create(PATH_BASE).setDefaultValue(new ModelNode(DEFAULT_PAGING_DIR)).build());
    }

    static final OperationStepHandler PATH_ADD = new OperationStepHandler() {

        @Override
        public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
            final Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
            final ModelNode model = resource.getModel();
            final String path = PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement().getValue();

            for (AttributeDefinition attribute : getAttributes(path)) {
                attribute.validateAndSet(operation, model);
            }
            reloadRequiredStep(context);
        }
    };

    static final OperationStepHandler PATH_REMOVE = new AbstractRemoveStepHandler() {

        @Override
        protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            super.performRemove(context, operation, model);
            reloadRequiredStep(context);
        }

    };

    private final PathElement path;

    static final AttributeDefinition[] getAttributes(final String path) {
        return new AttributeDefinition[] { PATHS.get(path), RELATIVE_TO };
    }

    static final PathDefinition BINDINGS_INSTANCE = new PathDefinition(MessagingExtension.BINDINGS_DIRECTORY_PATH);
    static final PathDefinition LARGE_MESSAGES_INSTANCE = new PathDefinition(MessagingExtension.LARGE_MESSAGES_DIRECTORY_PATH);
    static final PathDefinition PAGING_INSTANCE = new PathDefinition(MessagingExtension.PAGING_DIRECTORY_PATH);
    static final PathDefinition JOURNAL_INSTANCE = new PathDefinition(MessagingExtension.JOURNAL_DIRECTORY_PATH);

    public PathDefinition(PathElement path) {
        super(path,
                MessagingExtension.getResourceDescriptionResolver(ModelDescriptionConstants.PATH),
                PATH_ADD,
                PATH_REMOVE);
        this.path = path;
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(getAttributes(getPathElement().getValue()));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        AttributeDefinition[] attributes = getAttributes(path.getValue());
        OperationStepHandler attributeHandler = new ReloadRequiredWriteAttributeHandler(attributes);
        for (AttributeDefinition attribute : attributes) {
            registry.registerReadWriteAttribute(attribute, null, attributeHandler);
        }
    }

    // TODO add @Override once the WFCORE version with this method is integrated
    public int getMinOccurs() {
        return 1;
    }

    protected static void registerResolveOperationHandler(ExtensionContext context, ManagementResourceRegistration registry) {
        if (context.getProcessType().isServer()) {
            final ResolvePathHandler resolvePathHandler = ResolvePathHandler.Builder.of(context.getPathManager())
                    .setPathAttribute(PATHS.get(registry.getPathAddress().getLastElement().getValue()))
                    .setRelativeToAttribute(PathDefinition.RELATIVE_TO)
                    .setCheckAbsolutePath(true)
                    .build();
            registry.registerOperationHandler(resolvePathHandler.getOperationDefinition(), resolvePathHandler);
        }

    }

    static void reloadRequiredStep(final OperationContext context) {
        if(context.isNormalServer()) {
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                    final ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));
                    final ServiceController<?> controller = context.getServiceRegistry(false).getService(serviceName);
                    OperationContext.RollbackHandler rh;
                    if(controller != null) {
                        context.reloadRequired();
                        rh = OperationContext.RollbackHandler.REVERT_RELOAD_REQUIRED_ROLLBACK_HANDLER;
                    } else {
                        rh = OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER;
                    }
                    context.completeStep(rh);
                }
            }, OperationContext.Stage.RUNTIME);
        }
    }
}
