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
package org.jboss.as.domain.management.connections.ldap;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.LDAP_CONNECTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTY;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.ControllerResolver;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * A {@link ResourceDefinition} for additional properties that can be defined for a LDAP connection.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class LdapConnectionPropertyResourceDefinition extends SimpleResourceDefinition {

    static final PathElement RESOURCE_PATH = PathElement.pathElement(PROPERTY);

    public static final SimpleAttributeDefinition VALUE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.VALUE,
            ModelType.STRING, false).setAllowExpression(true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, true)).build();

    static final ResourceDefinition INSTANCE = new LdapConnectionPropertyResourceDefinition();

    private LdapConnectionPropertyResourceDefinition() {
        super(RESOURCE_PATH, ControllerResolver.getResolver("core.management.ldap-connection.property"),
                new PropertyAddHandler(), new PropertyRemoveHandler(), OperationEntry.Flag.RESTART_NONE,
                OperationEntry.Flag.RESTART_RESOURCE_SERVICES);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadWriteAttribute(VALUE, null, new ValueWriteHandler());
    }

    private static class PropertyManipulator {

        private final LdapConnectionManagerService service;
        private final boolean isBooting;
        private final String propertyName;

        private PropertyManipulator(OperationContext context, ModelNode operation) {
            PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));

            LdapConnectionManagerService service = null;
            String propertyName = null;

            for (PathElement current : address) {
                String currentKey = current.getKey();
                if (currentKey.equals(LDAP_CONNECTION)) {
                    String connectionName = current.getValue();
                    ServiceName svcName = LdapConnectionManagerService.ServiceUtil.createServiceName(connectionName);
                    ServiceRegistry registry = context.getServiceRegistry(true);
                    ServiceController<?> controller = registry.getService(svcName);
                    service = LdapConnectionManagerService.class.cast(controller.getValue());
                } else if (currentKey.equals(PROPERTY)) {
                    propertyName = current.getValue();
                }
            }

            this.service = service;
            this.propertyName = propertyName;
            this.isBooting = context.isBooting();
        }

        private void setValue(final String value) {
            if (isBooting) {
                service.setPropertyImmediate(propertyName, value);
            } else {
                service.setProperty(propertyName, value);
            }

        }

        private void remove() {
            service.removeProperty(propertyName);
        }

    }

    private static class PropertyAddHandler extends AbstractAddStepHandler {

        private PropertyAddHandler() {
            super(VALUE);
        }

        @Override
        protected boolean requiresRuntime(OperationContext context) {
            return true;
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model,
                ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers)
                throws OperationFailedException {
            new PropertyManipulator(context, operation).setValue(VALUE.resolveModelAttribute(context, model).asString());
        }

    }

    private static class PropertyRemoveHandler extends AbstractRemoveStepHandler {

        @Override
        protected boolean requiresRuntime(OperationContext context) {
            return true;
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
                throws OperationFailedException {
            new PropertyManipulator(context, operation).remove();
        }

    }

    private static class ValueWriteHandler extends AbstractWriteAttributeHandler<Void> {

        private ValueWriteHandler() {
            super(VALUE);
        }

        @Override
        protected boolean requiresRuntime(OperationContext context) {
            return true;
        }

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                ModelNode resolvedValue, ModelNode currentValue,
                org.jboss.as.controller.AbstractWriteAttributeHandler.HandbackHolder<Void> handbackHolder)
                throws OperationFailedException {
            new PropertyManipulator(context, operation).setValue(resolvedValue.asString());
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
            new PropertyManipulator(context, operation).setValue(valueToRestore.asString());
        }

    }

}
