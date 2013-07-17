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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.parsing.Attribute.APPLICATION;

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
import org.jboss.as.controller.access.constraint.ApplicationTypeConfig;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource.ResourceEntry;
import org.jboss.as.domain.management._private.DomainManagementResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ApplicationTypeConfigResourceDefinition extends SimpleResourceDefinition {

    public static PathElement PATH_ELEMENT = PathElement.pathElement(TYPE);

    public static SimpleAttributeDefinition DEFAULT_APPLICATION = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.DEFAULT_APPLICATION, ModelType.BOOLEAN, false)
            .setStorageRuntime()
            .build();

    public static SimpleAttributeDefinition CONFIGURED_APPLICATION = SimpleAttributeDefinitionBuilder.create(ModelDescriptionConstants.CONFIGURED_APPLICATION, ModelType.BOOLEAN, true)
            .setXmlName(APPLICATION.getLocalName())
//            .setAllowExpression(true)
            .build();

    ApplicationTypeConfigResourceDefinition() {
        super(PATH_ELEMENT, DomainManagementResolver.getResolver("core.access-constraint.application-type-config"));
    }

    static ResourceEntry createResource(ApplicationTypeConfig applicationType, String type, String name) {
        return createResource(applicationType, PathElement.pathElement(type, name));
    }

    static ResourceEntry createResource(ApplicationTypeConfig applicationType, PathElement pathElement) {
        return new ApplicationTypeConfigResource(pathElement, applicationType);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerReadOnlyAttribute(DEFAULT_APPLICATION, ApplicationTypeConfigReadAttributeHandler.INSTANCE);
        resourceRegistration.registerReadWriteAttribute(CONFIGURED_APPLICATION, ApplicationTypeConfigReadAttributeHandler.INSTANCE, ApplicationTypeConfigWriteAttributeHandler.INSTANCE);
    }

    private static class ApplicationTypeConfigReadAttributeHandler implements OperationStepHandler {

        static final ApplicationTypeConfigReadAttributeHandler INSTANCE = new ApplicationTypeConfigReadAttributeHandler();

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final String attribute = operation.require(NAME).asString();
            final ApplicationTypeConfigResource resource = (ApplicationTypeConfigResource)context.readResource(PathAddress.EMPTY_ADDRESS);
            final ApplicationTypeConfig applicationType = resource.applicationType;
            Boolean result = null;
            if (attribute.equals(DEFAULT_APPLICATION.getName())) {
                result = applicationType.isDefaultApplication();
            } else if (attribute.equals(CONFIGURED_APPLICATION.getName())) {
                result = applicationType.getConfiguredApplication();
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

    private static class ApplicationTypeConfigWriteAttributeHandler implements OperationStepHandler {

        static final ApplicationTypeConfigWriteAttributeHandler INSTANCE = new ApplicationTypeConfigWriteAttributeHandler();

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final String attribute = operation.require(NAME).asString();
            final ModelNode value = operation.require(VALUE);
            final ApplicationTypeConfigResource resource = (ApplicationTypeConfigResource)context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
            final ApplicationTypeConfig classification = resource.applicationType;
            if (attribute.equals(CONFIGURED_APPLICATION.getName())) {
                classification.setConfiguredApplication(readValue(context, value, CONFIGURED_APPLICATION));
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

    private static class ApplicationTypeConfigResource extends AbstractClassificationResource {
        private final ApplicationTypeConfig applicationType;

        ApplicationTypeConfigResource(PathElement pathElement, ApplicationTypeConfig classification) {
            super(pathElement);
            this.applicationType = classification;
        }

        @Override
        public ModelNode getModel() {
            ModelNode model = new ModelNode();
            model.get(DEFAULT_APPLICATION.getName()).set(applicationType.isDefaultApplication());
            model.get(CONFIGURED_APPLICATION.getName()).set(getBoolean(applicationType.getConfiguredApplication()));
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
