/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.jca;

import static org.jboss.as.connector.subsystems.jca.Constants.CACHED_CONNECTION_MANAGER;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class JcaCachedConnectionManagerDefinition extends SimpleResourceDefinition {
    protected static final PathElement PATH_CACHED_CONNECTION_MANAGER = PathElement.pathElement(CACHED_CONNECTION_MANAGER, CACHED_CONNECTION_MANAGER);

    JcaCachedConnectionManagerDefinition() {
        super(PATH_CACHED_CONNECTION_MANAGER,
                JcaExtension.getResourceDescriptionResolver(PATH_CACHED_CONNECTION_MANAGER.getKey()),
                 CachedConnectionManagerAdd.INSTANCE,
                 CachedConnectionManagerRemove.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        for (final CcmParameters parameter : CcmParameters.values()) {
            if (parameter != CcmParameters.INSTALL) {
                resourceRegistration.registerReadWriteAttribute(parameter.getAttribute(), null, JcaCachedConnectionManagerWriteHandler.INSTANCE);
            } else {
                AttributeDefinition ad = parameter.getAttribute();
                resourceRegistration.registerReadWriteAttribute(ad, null, new ReloadRequiredWriteAttributeHandler(ad));
            }
        }

    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
        resourceRegistration.registerOperationHandler(CcmOperations.GET_NUMBER_OF_CONNECTIONS.getOperation(), GetNumberOfConnectionsHandler.INSTANCE);
        resourceRegistration.registerOperationHandler(CcmOperations.LIST_CONNECTIONS.getOperation(), ListOfConnectionsHandler.INSTANCE);

    }

    static void registerTransformers110(ResourceTransformationDescriptionBuilder parentBuilder) {

            ResourceTransformationDescriptionBuilder builder = parentBuilder.addChildResource(PATH_CACHED_CONNECTION_MANAGER);
            builder.getAttributeBuilder().setDiscard(DiscardAttributeChecker.ALWAYS, CcmParameters.IGNORE_UNKNOWN_CONNECTIONS.getAttribute());

        }

    public enum CcmParameters {
        DEBUG(SimpleAttributeDefinitionBuilder.create("debug", ModelType.BOOLEAN)
                .setAllowExpression(true)
                .setRequired(false)
                .setDefaultValue(ModelNode.FALSE)
                .setMeasurementUnit(MeasurementUnit.NONE)
                .setRestartAllServices()
                .setXmlName("debug")
                .build()),
        ERROR(SimpleAttributeDefinitionBuilder.create("error", ModelType.BOOLEAN)
                .setAllowExpression(true)
                .setRequired(false)
                .setDefaultValue(ModelNode.FALSE)
                .setMeasurementUnit(MeasurementUnit.NONE)
                .setRestartAllServices()
                .setXmlName("error")
                .build()),
        IGNORE_UNKNOWN_CONNECTIONS(SimpleAttributeDefinitionBuilder.create("ignore-unknown-connections", ModelType.BOOLEAN)
                .setAllowExpression(true)
                .setRequired(false)
                .setDefaultValue(ModelNode.FALSE)
                .setMeasurementUnit(MeasurementUnit.NONE)
                .setRestartAllServices()
                .setXmlName("ignore-unknown-connections")
                .build()),
        INSTALL(SimpleAttributeDefinitionBuilder.create("install", ModelType.BOOLEAN)
                .setAllowExpression(false)
                .setRequired(false)
                .setDefaultValue(ModelNode.FALSE)
                .setMeasurementUnit(MeasurementUnit.NONE)
                .setRestartAllServices()
                .build());


        CcmParameters(SimpleAttributeDefinition attribute) {
            this.attribute = attribute;
        }

        public SimpleAttributeDefinition getAttribute() {
            return attribute;
        }

        private SimpleAttributeDefinition attribute;
    }


    public enum CcmOperations {
        GET_NUMBER_OF_CONNECTIONS(new SimpleOperationDefinitionBuilder("get-number-of-connections", JcaExtension.getResourceDescriptionResolver(PATH_CACHED_CONNECTION_MANAGER.getKey()))
                .setRuntimeOnly()
                .setReadOnly()
                .build()),
        LIST_CONNECTIONS(new SimpleOperationDefinitionBuilder("list-connections", JcaExtension.getResourceDescriptionResolver(PATH_CACHED_CONNECTION_MANAGER.getKey()))
                .setRuntimeOnly()
                .setReadOnly()
                .build());


        CcmOperations(SimpleOperationDefinition operation) {
            this.operation = operation;
        }

        public SimpleOperationDefinition getOperation() {
            return operation;
        }

        private SimpleOperationDefinition operation;
    }



}
