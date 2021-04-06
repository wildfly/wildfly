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

package org.wildfly.extension.messaging.activemq;

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.client.helpers.MeasurementUnit.BYTES;
import static org.jboss.dmr.ModelType.INT;
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

    private static final AttributeDefinition NUMBER_OF_PAGES = create(CommonAttributes.NUMBER_OF_PAGES, INT)
            .setStorageRuntime()
            .build();

    private static final AttributeDefinition NUMBER_OF_BYTES_PER_PAGE = create(CommonAttributes.NUMBER_OF_BYTES_PER_PAGE, LONG)
            .setMeasurementUnit(BYTES)
            .setStorageRuntime()
            .build();

    public static final AttributeDefinition[] ATTRS = { QUEUE_NAMES, BINDING_NAMES, NUMBER_OF_PAGES, NUMBER_OF_BYTES_PER_PAGE };

    static final CoreAddressDefinition INSTANCE = new CoreAddressDefinition();

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
        super.registerAttributes(registry);

        for (AttributeDefinition attr : ATTRS) {
            registry.registerReadOnlyAttribute(attr, AddressControlHandler.INSTANCE);
        }
    }

    @Override
    public void registerChildren(ManagementResourceRegistration registry) {
        super.registerChildren(registry);

        // TODO WFLY-5285 get rid of redundant .setRuntimeOnly once WFCORE-959 is integrated
        ManagementResourceRegistration securityRole = registry.registerSubModel(SecurityRoleDefinition.RUNTIME_INSTANCE);
        securityRole.setRuntimeOnly(true);
    }
}
