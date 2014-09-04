/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.host.controller.resources;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HTTP_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MANAGEMENT_INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;

import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.parsing.Attribute;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.host.controller.HostControllerEnvironment;
import org.jboss.as.host.controller.HostModelUtil;
import org.jboss.as.host.controller.operations.HttpManagementAddHandler;
import org.jboss.as.host.controller.operations.HttpManagementRemoveHandler;
import org.jboss.as.host.controller.operations.HttpManagementWriteAttributeHandler;
import org.jboss.as.host.controller.operations.LocalHostControllerInfoImpl;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link ResourceDefinition} for the HTTP management interface resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class HttpManagementResourceDefinition extends SimpleResourceDefinition {

    private final LocalHostControllerInfoImpl hostControllerInfo;
    private final HostControllerEnvironment environment;

    private static final PathElement RESOURCE_PATH = PathElement.pathElement(MANAGEMENT_INTERFACE, HTTP_INTERFACE);

    private static final OperationStepHandler VALIDATING_HANDLER = new HttpManagementValidatingHandler();

    public static final SimpleAttributeDefinition SECURITY_REALM = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SECURITY_REALM, ModelType.STRING, true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SECURITY_REALM_REF)
            .build();

    public static final SimpleAttributeDefinition INTERFACE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.INTERFACE, ModelType.STRING, false)
            // expressions only allowed due to compatibility. allowing this was a mistake
            .setAllowExpression(true)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, false, true))
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_CONFIG)
            .build();

    public static final SimpleAttributeDefinition HTTP_PORT = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PORT, ModelType.INT, true)
            .setAllowExpression(true).setValidator(new IntRangeValidator(0, 65535, true, true))
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_CONFIG)
            .build();

    public static final SimpleAttributeDefinition HTTPS_PORT = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SECURE_PORT, ModelType.INT, true)
            .setAllowExpression(true).setValidator(new IntRangeValidator(0, 65535, true, true))
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_CONFIG)
            .build();
    public static final SimpleAttributeDefinition SECURE_INTERFACE = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.SECURE_INTERFACE, ModelType.STRING, true)
            // SECURE_INTERFACE does not allow expressions. INTERFACE only does due to compatibility; otherwise it shouldn't
            .setAllowExpression(false)
            .setValidator(new StringLengthValidator(1, Integer.MAX_VALUE, true, false))
            .addAccessConstraint(SensitiveTargetAccessConstraintDefinition.SOCKET_CONFIG)
            .build();
    public static final SimpleAttributeDefinition CONSOLE_ENABLED = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.CONSOLE_ENABLED, ModelType.BOOLEAN, true)
                .setAllowExpression(true)
                .setXmlName(Attribute.CONSOLE_ENABLED.getLocalName())
                .setDefaultValue(new ModelNode(true))
                .build();

    public static final AttributeDefinition[] ATTRIBUTE_DEFINITIONS = new AttributeDefinition[] {INTERFACE, HTTP_PORT, HTTPS_PORT, SECURE_INTERFACE, SECURITY_REALM,CONSOLE_ENABLED};

    private final List<AccessConstraintDefinition> accessConstraints;

    public HttpManagementResourceDefinition(final LocalHostControllerInfoImpl hostControllerInfo,
                                             final HostControllerEnvironment environment) {
        super(RESOURCE_PATH,
                HostModelUtil.getResourceDescriptionResolver("core", "management", "http-interface"),
                new HttpManagementAddHandler(hostControllerInfo, environment),
                new HttpManagementRemoveHandler(hostControllerInfo, environment),
                OperationEntry.Flag.RESTART_NONE, OperationEntry.Flag.RESTART_NONE);
        this.hostControllerInfo = hostControllerInfo;
        this.environment = environment;
        this.accessConstraints = SensitiveTargetAccessConstraintDefinition.MANAGEMENT_INTERFACES.wrapAsList();
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        final HttpManagementWriteAttributeHandler writeAttributeHandler = new HttpManagementWriteAttributeHandler(hostControllerInfo, environment);

        for (AttributeDefinition attr : ATTRIBUTE_DEFINITIONS) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeAttributeHandler);
        }
    }

    @Override
    public List<AccessConstraintDefinition> getAccessConstraints() {
        return accessConstraints;
    }

    public static void addValidatingHandler(OperationContext operationContext, ModelNode fromOperation) {
        ModelNode operation = Util.createOperation("validate-http-interface", PathAddress.pathAddress(fromOperation.require(OP_ADDR)));

        operationContext.addStep(operation, VALIDATING_HANDLER, Stage.MODEL);
    }

    private static class HttpManagementValidatingHandler implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();

            if (model.hasDefined(SECURITY_REALM.getName()) == false && model.hasDefined(HTTPS_PORT.getName())) {
                throw MESSAGES.noSecurityRealmForSsl();
            }

            context.stepCompleted();
        }

    }

}
