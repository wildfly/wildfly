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
package org.jboss.as.domain.management.access;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONSTRAINT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT_EXPRESSION;
import static org.jboss.as.controller.parsing.Attribute.REQUIRES_ACCESS;
import static org.jboss.as.controller.parsing.Attribute.REQUIRES_READ;
import static org.jboss.as.controller.parsing.Attribute.REQUIRES_WRITE;

import java.util.Collections;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.access.constraint.AbstractSensitivity;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource.ResourceEntry;
import org.jboss.as.domain.management._private.DomainManagementResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class SensitivityResourceDefinition extends SimpleResourceDefinition {

    public static PathElement PATH_ELEMENT = PathElement.pathElement(TYPE);

    public static PathElement VAULT_ELEMENT = PathElement.pathElement(CONSTRAINT, VAULT_EXPRESSION);

    public static SimpleAttributeDefinition DEFAULT_REQUIRES_ACCESS = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.DEFAULT_REQUIRES_ACCESS, ModelType.BOOLEAN, false)
            .setStorageRuntime()
            .build();


    public static SimpleAttributeDefinition DEFAULT_REQUIRES_READ = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.DEFAULT_REQUIRES_READ, ModelType.BOOLEAN, false)
            .setStorageRuntime()
            .build();

    public static SimpleAttributeDefinition DEFAULT_REQUIRES_WRITE = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.DEFAULT_REQUIRES_WRITE, ModelType.BOOLEAN, false)
            .setStorageRuntime()
            .build();

    public static SimpleAttributeDefinition CONFIGURED_REQUIRES_ACCESS = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.CONFIGURED_REQUIRES_ACCESS, ModelType.BOOLEAN, true)
            .setXmlName(REQUIRES_ACCESS.getLocalName())
            .setAllowExpression(true)
            .build();


    public static SimpleAttributeDefinition CONFIGURED_REQUIRES_READ = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.CONFIGURED_REQUIRES_READ, ModelType.BOOLEAN, true)
            .setXmlName(REQUIRES_READ.getLocalName())
            .setAllowExpression(true)
            .build();

    public static SimpleAttributeDefinition CONFIGURED_REQUIRES_WRITE = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.CONFIGURED_REQUIRES_WRITE, ModelType.BOOLEAN, true)
            .setXmlName(REQUIRES_WRITE.getLocalName())
            .setAllowExpression(true)
            .build();

    private SensitivityResourceDefinition(PathElement pathElement, ResourceDescriptionResolver resolver) {
        super(pathElement, resolver);
    }

    static SensitivityResourceDefinition createSensitivityClassification() {
        return new SensitivityResourceDefinition(PATH_ELEMENT, DomainManagementResolver.getResolver("core.access-control.constraint.sensitivity-classification-config"));
    }

    static SensitivityResourceDefinition createVaultExpressionConfiguration() {
        return new SensitivityResourceDefinition(VAULT_ELEMENT, DomainManagementResolver.getResolver("core.access-control.constraint.vault-expression-sensitivity"));
    }

    static ResourceEntry createResource(AbstractSensitivity classification, String type, String name) {
        return createResource(classification, PathElement.pathElement(type, name));
    }

    static ResourceEntry createResource(AbstractSensitivity classification, PathElement pathElement) {
        return new SensitivityClassificationResource(pathElement, classification);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(DEFAULT_REQUIRES_ACCESS, SensitivityClassificationReadAttributeHandler.INSTANCE);
        resourceRegistration.registerReadOnlyAttribute(DEFAULT_REQUIRES_READ, SensitivityClassificationReadAttributeHandler.INSTANCE);
        resourceRegistration.registerReadOnlyAttribute(DEFAULT_REQUIRES_WRITE, SensitivityClassificationReadAttributeHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(CONFIGURED_REQUIRES_ACCESS, SensitivityClassificationReadAttributeHandler.INSTANCE, SensitivityClassificationWriteAttributeHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(CONFIGURED_REQUIRES_READ, SensitivityClassificationReadAttributeHandler.INSTANCE, SensitivityClassificationWriteAttributeHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(CONFIGURED_REQUIRES_WRITE, SensitivityClassificationReadAttributeHandler.INSTANCE, SensitivityClassificationWriteAttributeHandler.INSTANCE);
    }

    private static class SensitivityClassificationReadAttributeHandler implements OperationStepHandler {

        static final SensitivityClassificationReadAttributeHandler INSTANCE = new SensitivityClassificationReadAttributeHandler();

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final String attribute = operation.require(NAME).asString();
            final SensitivityClassificationResource resource = (SensitivityClassificationResource)context.readResource(PathAddress.EMPTY_ADDRESS);
            final AbstractSensitivity classification = resource.classification;
            Boolean result = null;
            if (attribute.equals(DEFAULT_REQUIRES_ACCESS.getName())) {
                result = classification.isDefaultRequiresAccessPermission();
            } else if (attribute.equals(DEFAULT_REQUIRES_READ.getName())) {
                result = classification.isDefaultRequiresReadPermission();
            } else if (attribute.equals(DEFAULT_REQUIRES_WRITE.getName())) {
                result = classification.isDefaultRequiresWritePermission();
            } else if (attribute.equals(CONFIGURED_REQUIRES_ACCESS.getName())) {
                result = classification.getConfiguredRequiresAccessPermission();
            } else if (attribute.equals(CONFIGURED_REQUIRES_READ.getName())) {
                result = classification.getConfiguredRequiresReadPermission();
            } else if (attribute.equals(CONFIGURED_REQUIRES_WRITE.getName())) {
                result = classification.getConfiguredRequiresWritePermission();
            } else {
                //TODO i18n
                throw new IllegalStateException();
            }
            if (result != null) {
                context.getResult().set(result);
            }
            context.stepCompleted();
        }
    }

    private static class SensitivityClassificationWriteAttributeHandler implements OperationStepHandler {

        static final SensitivityClassificationWriteAttributeHandler INSTANCE = new SensitivityClassificationWriteAttributeHandler();

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final String attribute = operation.require(NAME).asString();
            final ModelNode value = operation.require(VALUE);
            final SensitivityClassificationResource resource = (SensitivityClassificationResource)context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
            final AbstractSensitivity classification = resource.classification;
            if (attribute.equals(CONFIGURED_REQUIRES_ACCESS.getName())) {
                classification.setConfiguredRequiresAccessPermission(readValue(context, value, CONFIGURED_REQUIRES_ACCESS));
            } else if (attribute.equals(CONFIGURED_REQUIRES_READ.getName())) {
                classification.setConfiguredRequiresReadPermission(readValue(context, value, CONFIGURED_REQUIRES_READ));
            } else if (attribute.equals(CONFIGURED_REQUIRES_WRITE.getName())) {
                classification.setConfiguredRequiresWritePermission(readValue(context, value, CONFIGURED_REQUIRES_WRITE));
            } else {
                //TODO i18n
                throw new IllegalStateException();
            }
            context.stepCompleted();
        }

        private Boolean readValue(OperationContext context, ModelNode value, AttributeDefinition definition) throws OperationFailedException {
            if (value.isDefined()) {
                return definition.resolveValue(context, value).asBoolean();
            }
            return null;
        }
    }

    private static class SensitivityClassificationResource extends AbstractClassificationResource {
        private final AbstractSensitivity classification;

        SensitivityClassificationResource(PathElement pathElement, AbstractSensitivity classification) {
            super(pathElement);
            this.classification = classification;
        }

        @Override
        public ModelNode getModel() {
            ModelNode model = new ModelNode();
            model.get(DEFAULT_REQUIRES_ACCESS.getName()).set(classification.isDefaultRequiresAccessPermission());
            model.get(DEFAULT_REQUIRES_READ.getName()).set(classification.isDefaultRequiresReadPermission());
            model.get(DEFAULT_REQUIRES_WRITE.getName()).set(classification.isDefaultRequiresWritePermission());
            model.get(CONFIGURED_REQUIRES_ACCESS.getName()).set(getBoolean(classification.getConfiguredRequiresAccessPermission()));
            model.get(CONFIGURED_REQUIRES_READ.getName()).set(getBoolean(classification.getConfiguredRequiresReadPermission()));
            model.get(CONFIGURED_REQUIRES_WRITE.getName()).set(getBoolean(classification.getConfiguredRequiresWritePermission()));
            return model;
        }

        private ModelNode getBoolean(Boolean booleanValue) {
            if (booleanValue == null) {
                return new ModelNode();
            }
            return new ModelNode(booleanValue);
        }


        @Override
        public Set<String> getChildTypes() {
            return Collections.emptySet();
        }


        @Override
        ResourceEntry getChildEntry(String type, String name) {
            return null;
        }

        @Override
        public Set<String> getChildrenNames(String type) {
            return Collections.emptySet();
        }

        @Override
        public Set<ResourceEntry> getChildren(String childType) {
            return Collections.emptySet();
        }

    }
}
