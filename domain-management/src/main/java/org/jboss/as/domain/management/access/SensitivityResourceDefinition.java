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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.APPLIES_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CLASSIFICATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CONSTRAINT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VAULT_EXPRESSION;
import static org.jboss.as.controller.parsing.Attribute.REQUIRES_ADDRESSABLE;
import static org.jboss.as.controller.parsing.Attribute.REQUIRES_READ;
import static org.jboss.as.controller.parsing.Attribute.REQUIRES_WRITE;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.jboss.as.controller.access.management.AccessConstraintKey;
import org.jboss.as.controller.access.management.AccessConstraintUtilization;
import org.jboss.as.controller.access.management.AccessConstraintUtilizationRegistry;
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

    public static PathElement PATH_ELEMENT = PathElement.pathElement(CLASSIFICATION);

    public static PathElement VAULT_ELEMENT = PathElement.pathElement(CONSTRAINT, VAULT_EXPRESSION);

    public static SimpleAttributeDefinition DEFAULT_REQUIRES_ADDRESSABLE = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.DEFAULT_REQUIRES_ADDRESSABLE, ModelType.BOOLEAN, false)
            .setStorageRuntime()
            .build();


    public static SimpleAttributeDefinition DEFAULT_REQUIRES_READ = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.DEFAULT_REQUIRES_READ, ModelType.BOOLEAN, false)
            .setStorageRuntime()
            .build();

    public static SimpleAttributeDefinition DEFAULT_REQUIRES_WRITE = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.DEFAULT_REQUIRES_WRITE, ModelType.BOOLEAN, false)
            .setStorageRuntime()
            .build();

    public static SimpleAttributeDefinition CONFIGURED_REQUIRES_ADDRESSABLE = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.CONFIGURED_REQUIRES_ADDRESSABLE, ModelType.BOOLEAN, true)
            .setXmlName(REQUIRES_ADDRESSABLE.getLocalName())
//            .setAllowExpression(true)
            .build();


    public static SimpleAttributeDefinition CONFIGURED_REQUIRES_READ = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.CONFIGURED_REQUIRES_READ, ModelType.BOOLEAN, true)
            .setXmlName(REQUIRES_READ.getLocalName())
//            .setAllowExpression(true)
            .build();

    public static SimpleAttributeDefinition CONFIGURED_REQUIRES_WRITE = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.CONFIGURED_REQUIRES_WRITE, ModelType.BOOLEAN, true)
            .setXmlName(REQUIRES_WRITE.getLocalName())
//            .setAllowExpression(true)
            .build();

    public static List<AttributeDefinition> getWritableVaultAttributeDefinitions() {
        return Arrays.asList((AttributeDefinition) CONFIGURED_REQUIRES_READ,
                (AttributeDefinition) CONFIGURED_REQUIRES_WRITE);
    }

    private final boolean includeAddressable;
    private final boolean registerUtilization;

    private SensitivityResourceDefinition(PathElement pathElement, ResourceDescriptionResolver resolver,
                                          boolean includeAddressable, boolean registerUtilization) {
        super(pathElement, resolver);
        this.includeAddressable = includeAddressable;
        this.registerUtilization = registerUtilization;
    }

    static SensitivityResourceDefinition createSensitivityClassification() {
        return new SensitivityResourceDefinition(PATH_ELEMENT, DomainManagementResolver.getResolver("core.access-control.constraint.sensitivity-classification-config"), true, true);
    }

    static SensitivityResourceDefinition createVaultExpressionConfiguration() {
        return new SensitivityResourceDefinition(VAULT_ELEMENT, DomainManagementResolver.getResolver("core.access-control.constraint.vault-expression-sensitivity"), false, false);
    }

    static ResourceEntry createVaultExpressionResource(AbstractSensitivity classification, PathElement pathElement) {
        return new SensitivityClassificationResource(pathElement, classification);
    }

    static ResourceEntry createSensitivityClassificationResource(AbstractSensitivity classification, String classificationType, String name,
                                                                 AccessConstraintUtilizationRegistry registry) {
        return new SensitivityClassificationResource(PathElement.pathElement(ModelDescriptionConstants.CLASSIFICATION, name), classification, classificationType, registry);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        OperationStepHandler read = includeAddressable ? SensitivityClassificationReadAttributeHandler.ADDRESSABLE_INSTANCE : SensitivityClassificationReadAttributeHandler.NON_ADDRESSABLE_INSTANCE;
        OperationStepHandler write = includeAddressable ? SensitivityClassificationWriteAttributeHandler.ADDRESSABLE_INSTANCE : SensitivityClassificationWriteAttributeHandler.NON_ADDRESSABLE_INSTANCE;
        if (includeAddressable) {
            resourceRegistration.registerReadOnlyAttribute(DEFAULT_REQUIRES_ADDRESSABLE, read);
        }
        resourceRegistration.registerReadOnlyAttribute(DEFAULT_REQUIRES_READ, read);
        resourceRegistration.registerReadOnlyAttribute(DEFAULT_REQUIRES_WRITE, read);
        if (includeAddressable) {
            resourceRegistration.registerReadWriteAttribute(CONFIGURED_REQUIRES_ADDRESSABLE, read, write);
        }
        resourceRegistration.registerReadWriteAttribute(CONFIGURED_REQUIRES_READ, SensitivityClassificationReadAttributeHandler.ADDRESSABLE_INSTANCE, write);
        resourceRegistration.registerReadWriteAttribute(CONFIGURED_REQUIRES_WRITE, SensitivityClassificationReadAttributeHandler.ADDRESSABLE_INSTANCE, write);
    }

    @Override
    public void registerChildren(ManagementResourceRegistration resourceRegistration) {
        if (registerUtilization) {
            resourceRegistration.registerSubModel(new AccessConstraintAppliesToResourceDefinition());
        }
    }

    private static class SensitivityClassificationReadAttributeHandler implements OperationStepHandler {

        static final SensitivityClassificationReadAttributeHandler ADDRESSABLE_INSTANCE = new SensitivityClassificationReadAttributeHandler(true);
        static final SensitivityClassificationReadAttributeHandler NON_ADDRESSABLE_INSTANCE = new SensitivityClassificationReadAttributeHandler(false);

        private final boolean includeAddressable;

        public SensitivityClassificationReadAttributeHandler(boolean includeAddressable) {
            this.includeAddressable = includeAddressable;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final String attribute = operation.require(NAME).asString();
            final SensitivityClassificationResource resource = (SensitivityClassificationResource)context.readResource(PathAddress.EMPTY_ADDRESS);
            final AbstractSensitivity classification = resource.classification;
            Boolean result;
            if (attribute.equals(DEFAULT_REQUIRES_ADDRESSABLE.getName()) && includeAddressable) {
                result = classification.isDefaultRequiresAccessPermission();
            } else if (attribute.equals(DEFAULT_REQUIRES_READ.getName())) {
                result = classification.isDefaultRequiresReadPermission();
            } else if (attribute.equals(DEFAULT_REQUIRES_WRITE.getName())) {
                result = classification.isDefaultRequiresWritePermission();
            } else if (attribute.equals(CONFIGURED_REQUIRES_ADDRESSABLE.getName()) && includeAddressable) {
                result = classification.getConfiguredRequiresAccessPermission();
            } else if (attribute.equals(CONFIGURED_REQUIRES_READ.getName())) {
                result = classification.getConfiguredRequiresReadPermission();
            } else if (attribute.equals(CONFIGURED_REQUIRES_WRITE.getName())) {
                result = classification.getConfiguredRequiresWritePermission();
            } else {
                //TODO i18n
                throw new IllegalStateException();
            }

            context.getResult();
            if (result != null) {
                context.getResult().set(result);
            }
            context.stepCompleted();
        }
    }

    private static class SensitivityClassificationWriteAttributeHandler implements OperationStepHandler {

        static final SensitivityClassificationWriteAttributeHandler ADDRESSABLE_INSTANCE = new SensitivityClassificationWriteAttributeHandler(true);
        static final SensitivityClassificationWriteAttributeHandler NON_ADDRESSABLE_INSTANCE = new SensitivityClassificationWriteAttributeHandler(false);

        private final boolean includeAddressable;

        SensitivityClassificationWriteAttributeHandler(boolean includeAddressable){
            this.includeAddressable = includeAddressable;
        }
        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final String attribute = operation.require(NAME).asString();
            final ModelNode value = operation.require(VALUE);
            final SensitivityClassificationResource resource = (SensitivityClassificationResource)context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
            final AbstractSensitivity classification = resource.classification;
            if (attribute.equals(CONFIGURED_REQUIRES_ADDRESSABLE.getName()) && includeAddressable) {
                classification.setConfiguredRequiresAccessPermission(readValue(context, value, CONFIGURED_REQUIRES_ADDRESSABLE));
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
        private final AccessConstraintUtilizationRegistry registry;
        private final boolean includeAddressable;
        private final String classificationType;

        SensitivityClassificationResource(PathElement pathElement, AbstractSensitivity classification) {
            super(pathElement);
            this.classification = classification;
            this.includeAddressable = false;
            this.registry = null;
            this.classificationType = null;
        }

        SensitivityClassificationResource(PathElement pathElement, AbstractSensitivity classification,
                                          String classificationType, AccessConstraintUtilizationRegistry registry) {
            super(pathElement);
            this.classification = classification;
            this.includeAddressable = true;
            this.registry = registry;
            this.classificationType = classificationType;
        }

        @Override
        public ModelNode getModel() {
            ModelNode model = new ModelNode();
            if (includeAddressable) {
                model.get(DEFAULT_REQUIRES_ADDRESSABLE.getName()).set(classification.isDefaultRequiresAccessPermission());
            }
            model.get(DEFAULT_REQUIRES_READ.getName()).set(classification.isDefaultRequiresReadPermission());
            model.get(DEFAULT_REQUIRES_WRITE.getName()).set(classification.isDefaultRequiresWritePermission());
            if (includeAddressable) {
                model.get(CONFIGURED_REQUIRES_ADDRESSABLE.getName()).set(getBoolean(classification.getConfiguredRequiresAccessPermission()));
            }
            model.get(CONFIGURED_REQUIRES_READ.getName()).set(getBoolean(classification.getConfiguredRequiresReadPermission()));
            model.get(CONFIGURED_REQUIRES_WRITE.getName()).set(getBoolean(classification.getConfiguredRequiresWritePermission()));
            return model;
        }

        @Override
        public void writeModel(ModelNode newModel) {

            // Called on a slave host controller during boot
            if (includeAddressable) {
                if (newModel.hasDefined(CONFIGURED_REQUIRES_ADDRESSABLE.getName())) {
                    boolean b = newModel.get(CONFIGURED_REQUIRES_ADDRESSABLE.getName()).asBoolean();
                    classification.setConfiguredRequiresAccessPermission(b);
                }
            }
            if (newModel.hasDefined(CONFIGURED_REQUIRES_READ.getName())) {
                boolean b = newModel.get(CONFIGURED_REQUIRES_READ.getName()).asBoolean();
                classification.setConfiguredRequiresReadPermission(b);
            }
            if (newModel.hasDefined(CONFIGURED_REQUIRES_WRITE.getName())) {
                boolean b = newModel.get(CONFIGURED_REQUIRES_WRITE.getName()).asBoolean();
                classification.setConfiguredRequiresWritePermission(b);
            }
        }

        @Override
        public boolean isModelDefined() {
            return true;
        }

        private ModelNode getBoolean(Boolean booleanValue) {
            if (booleanValue == null) {
                return new ModelNode();
            }
            return new ModelNode(booleanValue);
        }


        @Override
        public Set<String> getChildTypes() {
            return registry == null ? Collections.<String>emptySet() : Collections.singleton(APPLIES_TO);
        }


        @Override
        ResourceEntry getChildEntry(String type, String name) {
            if (registry != null && APPLIES_TO.equals(type)) {
                Map<PathAddress, AccessConstraintUtilization> utilizations = getAccessConstraintUtilizations();
                for (AccessConstraintUtilization acu : utilizations.values()) {
                    if (name.equals(acu.getPathAddress().toCLIStyleString())) {
                        return AccessConstraintAppliesToResourceDefinition.createResource(acu);
                    }
                }
            }
            return null;
        }

        @Override
        public Set<String> getChildrenNames(String type) {
            if (registry != null && APPLIES_TO.equals(type)) {
                Map<PathAddress, AccessConstraintUtilization> utilizations = getAccessConstraintUtilizations();
                Set<String> result = new HashSet<String>();
                for (PathAddress pa : utilizations.keySet()) {
                    result.add(pa.toCLIStyleString());
                }
                return result;
            }
            return Collections.emptySet();
        }

        @Override
        public Set<ResourceEntry> getChildren(String childType) {
            if (registry != null && APPLIES_TO.equals(childType)) {
                Map<PathAddress, AccessConstraintUtilization> utilizations = getAccessConstraintUtilizations();
                Set<ResourceEntry> result = new HashSet<ResourceEntry>();
                for (AccessConstraintUtilization acu : utilizations.values()) {
                    result.add(AccessConstraintAppliesToResourceDefinition.createResource(acu));
                }
                return result;
            }
            return Collections.emptySet();
        }

        private Map<PathAddress, AccessConstraintUtilization> getAccessConstraintUtilizations() {
            boolean core = ModelDescriptionConstants.CORE.equals(classificationType);
            AccessConstraintKey key =
                    new AccessConstraintKey(ModelDescriptionConstants.SENSITIVITY_CLASSIFICATION,
                            core, core ? null : classificationType, getPathElement().getValue());
            return registry.getAccessConstraintUtilizations(key);
        }

    }
}
