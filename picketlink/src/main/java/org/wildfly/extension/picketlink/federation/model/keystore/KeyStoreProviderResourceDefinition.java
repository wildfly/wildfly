/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.picketlink.federation.model.keystore;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.wildfly.extension.picketlink.common.model.ModelElement;
import org.wildfly.extension.picketlink.federation.model.AbstractFederationResourceDefinition;
import org.wildfly.extension.picketlink.federation.service.KeyStoreProviderService;

import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 * @since Mar 16, 2012
 */
public class KeyStoreProviderResourceDefinition extends AbstractFederationResourceDefinition {

    public static final SimpleAttributeDefinition FILE = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_FILE.getName(), ModelType.STRING, false)
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition RELATIVE_TO = new SimpleAttributeDefinitionBuilder(ModelElement.COMMON_RELATIVE_TO.getName(), ModelType.STRING, true)
        .setRequires(ModelElement.COMMON_FILE.getName())
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition PASSWORD = new SimpleAttributeDefinitionBuilder(ModelElement.KEY_STORE_PASSWORD.getName(), ModelType.STRING, false)
        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition SIGN_KEY_ALIAS = new SimpleAttributeDefinitionBuilder(ModelElement.KEY_STORE_SIGN_KEY_ALIAS.getName(), ModelType.STRING, false)
        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
        .setAllowExpression(true)
        .build();
    public static final SimpleAttributeDefinition SIGN_KEY_PASSWORD = new SimpleAttributeDefinitionBuilder(ModelElement.KEY_STORE_SIGN_KEY_PASSWORD.getName(), ModelType.STRING, false)
        .setAccessConstraints(SensitiveTargetAccessConstraintDefinition.CREDENTIAL)
        .setAllowExpression(true)
        .build();

    public static final KeyStoreProviderResourceDefinition INSTANCE = new KeyStoreProviderResourceDefinition();

    private KeyStoreProviderResourceDefinition() {
        super(ModelElement.KEY_STORE, ModelElement.KEY_STORE.getName(), KeyStoreProviderAddHandler.INSTANCE, KeyStoreProviderRemoveHandler.INSTANCE, FILE, RELATIVE_TO, PASSWORD, SIGN_KEY_ALIAS, SIGN_KEY_PASSWORD);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        addChildResourceDefinition(KeyResourceDefinition.INSTANCE, resourceRegistration);
    }

    @Override
    protected OperationStepHandler createAttributeWriterHandler() {
        List<SimpleAttributeDefinition> attributes = getAttributes();
        return new AbstractWriteAttributeHandler(attributes.toArray(new AttributeDefinition[attributes.size()])) {
            @Override
            protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder handbackHolder) throws OperationFailedException {
                PathAddress pathAddress = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));

                updateConfiguration(context, pathAddress, false);

                return false;
            }

            @Override
            protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Object handback) throws OperationFailedException {
                PathAddress pathAddress = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));

                updateConfiguration(context, pathAddress, true);
            }

            private void updateConfiguration(OperationContext context, PathAddress pathAddress, boolean rollback) throws OperationFailedException {
                String federationAlias = pathAddress.subAddress(0, pathAddress.size() - 1).getLastElement().getValue();
                ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
                ServiceController<KeyStoreProviderService> serviceController =
                        (ServiceController<KeyStoreProviderService>) serviceRegistry.getService(KeyStoreProviderService.createServiceName(federationAlias));

                if (serviceController != null) {
                    KeyStoreProviderService service = serviceController.getValue();
                    ModelNode keyStoreProviderNode;

                    if (!rollback) {
                        keyStoreProviderNode = context.readResource(PathAddress.EMPTY_ADDRESS, false).getModel();
                    } else {
                        Resource rc = context.getOriginalRootResource().navigate(pathAddress);
                        keyStoreProviderNode = rc.getModel();
                    }

                    ModelNode relativeToNode = KeyStoreProviderResourceDefinition.RELATIVE_TO.resolveModelAttribute(context,
                            keyStoreProviderNode);
                    String relativeTo = null;

                    if (relativeToNode.isDefined()) {
                        relativeTo = relativeToNode.asString();
                    }

                    String file = KeyStoreProviderResourceDefinition.FILE.resolveModelAttribute(context, keyStoreProviderNode).asString();

                    service.setKeyProviderType(KeyStoreProviderAddHandler.toKeyProviderType(context, keyStoreProviderNode), file, relativeTo);
                }
            }
        };
    }
}
