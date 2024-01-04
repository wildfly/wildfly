/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.BYTES;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.STRING;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PrimitiveListAttributeDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Core address resource definition
 *
 * @author <a href="http://jmesnil.net">Jeff Mesnil</a> (c) 2012 Red Hat Inc.
 */
public class CoreAddressDefinition extends SimpleResourceDefinition {

    public static final PathElement PATH = PathElement.pathElement(CommonAttributes.CORE_ADDRESS);

    private static final AttributeDefinition QUEUE_NAMES = PrimitiveListAttributeDefinition.Builder.of(CommonAttributes.QUEUE_NAMES, STRING)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    private static final AttributeDefinition BINDING_NAMES = PrimitiveListAttributeDefinition.Builder.of(CommonAttributes.BINDING_NAMES, STRING)
            .setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    private static final AttributeDefinition NUMBER_OF_PAGES = create(CommonAttributes.NUMBER_OF_PAGES, LONG)
            .setStorageRuntime()
            .build();

    private static final AttributeDefinition NUMBER_OF_BYTES_PER_PAGE = create(CommonAttributes.NUMBER_OF_BYTES_PER_PAGE, LONG)
            .setMeasurementUnit(BYTES)
            .setStorageRuntime()
            .build();

    public static final AttributeDefinition[] ATTRS = { QUEUE_NAMES, BINDING_NAMES, NUMBER_OF_PAGES, NUMBER_OF_BYTES_PER_PAGE };

    public CoreAddressDefinition() {
        super(new Parameters(PATH,
                MessagingExtension.getResourceDescriptionResolver(CommonAttributes.CORE_ADDRESS)).setRuntime());
    }

    @Override
    public boolean isRuntime() {
        return true;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration registry) {
        for (AttributeDefinition attr : ATTRS) {
            registry.registerReadOnlyAttribute(attr, AddressControlHandler.INSTANCE);
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registry) {
        registry.registerSubModel(new SecurityRoleDefinition(true));
    }
}
