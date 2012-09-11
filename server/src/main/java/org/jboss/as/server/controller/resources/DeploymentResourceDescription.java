/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.server.controller.resources;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEPLOYMENT;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReadResourceNameOperationStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.MinMaxValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.server.controller.descriptions.ServerDescriptions;
import org.jboss.as.server.deployment.AbstractDeploymentUnitService;
import org.jboss.as.server.deployment.DeploymentStatusHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class DeploymentResourceDescription extends SimpleResourceDefinition {

    public static final AttributeDefinition NAME = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.NAME, ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1, false))
            .build();
    public static final AttributeDefinition RUNTIME_NAME = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.RUNTIME_NAME, ModelType.STRING, true)
            .setValidator(new StringLengthValidator(1, true))
            .build();
    public static final AttributeDefinition ENABLED = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.ENABLED, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode(false))
            .build();
    public static final AttributeDefinition PERSISTENT = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.PERSISTENT, ModelType.BOOLEAN, false)
            .build();
    public static final AttributeDefinition STATUS = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.STATUS, ModelType.STRING, true)
            .setValidator(new EnumValidator<AbstractDeploymentUnitService.DeploymentStatus>(AbstractDeploymentUnitService.DeploymentStatus.class, true))
            .build();

    //Managed content attributes
    private static final AttributeDefinition CONTENT_INPUT_STREAM_INDEX = createContentValueTypeAttribute(ModelDescriptionConstants.INPUT_STREAM_INDEX, ModelType.INT, new StringLengthValidator(1, true, true), true);
    private static final AttributeDefinition CONTENT_HASH = createContentValueTypeAttribute(ModelDescriptionConstants.HASH, ModelType.BYTES, new HashValidator(true), false);
    private static final AttributeDefinition CONTENT_BYTES = createContentValueTypeAttribute(ModelDescriptionConstants.BYTES, ModelType.BYTES, new ModelTypeValidator(ModelType.BYTES, true), false);
    private static final AttributeDefinition CONTENT_URL = createContentValueTypeAttribute(ModelDescriptionConstants.URL, ModelType.STRING, new StringLengthValidator(1, true), false);
    //Unmanaged content attributes
    private static final AttributeDefinition CONTENT_PATH = createContentValueTypeAttribute(ModelDescriptionConstants.PATH, ModelType.STRING, new StringLengthValidator(1, true), false);
    private static final AttributeDefinition CONTENT_RELATIVE_TO = createContentValueTypeAttribute(ModelDescriptionConstants.RELATIVE_TO, ModelType.STRING, new StringLengthValidator(1, true), false);
    private static final AttributeDefinition CONTENT_ARCHIVE = createContentValueTypeAttribute(ModelDescriptionConstants.ARCHIVE, ModelType.STRING, new StringLengthValidator(1, true), false);

    public static ObjectListAttributeDefinition CONTENT = ObjectListAttributeDefinition.Builder.of(ModelDescriptionConstants.CONTENT,
            ObjectTypeAttributeDefinition.Builder.of(ModelDescriptionConstants.CONTENT,
                    CONTENT_INPUT_STREAM_INDEX,
                    CONTENT_HASH,
                    CONTENT_BYTES,
                    CONTENT_URL,
                    CONTENT_PATH,
                    CONTENT_RELATIVE_TO,
                    CONTENT_ARCHIVE).build()).build();

    private DeploymentResourceParent parent;

    public static AttributeDefinition[] SERVER_ATTRIBUTES = new AttributeDefinition[] {NAME, RUNTIME_NAME, CONTENT, ENABLED, PERSISTENT, STATUS};
    public static AttributeDefinition[] SERVER_GROUP_ATTRIBUTES = new AttributeDefinition[] {NAME, RUNTIME_NAME, ENABLED};
    public static AttributeDefinition[] DOMAIN_ATTRIBUTES = new AttributeDefinition[] {NAME, RUNTIME_NAME, CONTENT};


    public DeploymentResourceDescription(DeploymentResourceParent parent, OperationStepHandler addHandler, OperationStepHandler removeHandler) {
        super(PathElement.pathElement(DEPLOYMENT),
                ServerDescriptions.getResourceDescriptionResolver(DEPLOYMENT, false),
                addHandler,
                removeHandler);
        this.parent = parent;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        for (AttributeDefinition attr : parent.getAttributes()) {
            if (attr.getName().equals(STATUS.getName())) {
                resourceRegistration.registerMetric(attr, DeploymentStatusHandler.INSTANCE);
            } else if (attr.getName().equals(NAME.getName())) {
                resourceRegistration.registerReadOnlyAttribute(attr, ReadResourceNameOperationStepHandler.INSTANCE);
            } else {
                resourceRegistration.registerReadOnlyAttribute(attr, null);
            }
        }
    }

    private static AttributeDefinition createContentValueTypeAttribute(String name, ModelType type, ParameterValidator validator, boolean allowExpression) {
        SimpleAttributeDefinitionBuilder builder = SimpleAttributeDefinitionBuilder.create(name, type, true);
        if (validator != null) {
            builder.setValidator(validator);
        }
        builder.setAllowExpression(allowExpression);
        return builder.build();
    }

    protected DeploymentResourceParent getParent() {
        return parent;
    }

    public static enum DeploymentResourceParent {
        DOMAIN (DOMAIN_ATTRIBUTES),
        SERVER_GROUP (SERVER_GROUP_ATTRIBUTES),
        SERVER (SERVER_ATTRIBUTES);

        final AttributeDefinition[] defs;
        private DeploymentResourceParent(AttributeDefinition[] defs) {
            this.defs = defs;
        }

        AttributeDefinition[] getAttributes() {
            return defs;
        }
    }

    private static class HashValidator extends ModelTypeValidator implements MinMaxValidator {
        public HashValidator(boolean nillable) {
            super(ModelType.BYTES, nillable);
        }

        @Override
        public Long getMin() {
            return 20L;
        }

        @Override
        public Long getMax() {
            return 20L;
        }
    }

}
