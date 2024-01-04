/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.DynamicNameMappers;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.undertow.filters.FilterRefDefinition;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class LocationDefinition extends PersistentResourceDefinition {
    static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.LOCATION);
    static final RuntimeCapability<Void> LOCATION_CAPABILITY = RuntimeCapability.Builder.of(Capabilities.CAPABILITY_LOCATION, true, LocationService.class)
            .addRequirements(Capabilities.CAPABILITY_UNDERTOW)
            .setDynamicNameMapper(DynamicNameMappers.GRAND_PARENT)
            .build();

    static final AttributeDefinition HANDLER = new SimpleAttributeDefinitionBuilder(Constants.HANDLER, ModelType.STRING)
            .setRequired(true)
            .setValidator(new StringLengthValidator(1))
            .setCapabilityReference(Capabilities.CAPABILITY_HANDLER)
            .setRestartAllServices()
            .build();

    static final Collection<AttributeDefinition> ATTRIBUTES = List.of(HANDLER);

    LocationDefinition() {
        super(new SimpleResourceDefinition.Parameters(PATH_ELEMENT, UndertowExtension.getResolver(Constants.HOST, PATH_ELEMENT.getKey()))
                .setAddHandler(LocationAdd.INSTANCE)
                .setRemoveHandler( new ServiceRemoveStepHandler(LocationAdd.INSTANCE) {
                    @Override
                    protected ServiceName serviceName(String name, PathAddress address) {
                        return LOCATION_CAPABILITY.getCapabilityServiceName(address);
                    }
                })
                .addCapabilities(LOCATION_CAPABILITY)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    @Override
    public List<? extends PersistentResourceDefinition> getChildren() {
        return List.of(new FilterRefDefinition());
    }
}
