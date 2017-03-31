/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.undertow.filters.FilterRefDefinition;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class LocationDefinition extends PersistentResourceDefinition {
    static final RuntimeCapability<Void> LOCATION_CAPABILITY = RuntimeCapability.Builder.of(Capabilities.CAPABILITY_LOCATION, true, LocationService.class)
            .addRequirements(Capabilities.CAPABILITY_UNDERTOW)
            .setDynamicNameMapper(path -> new String[]{
                    path.getParent().getParent().getLastElement().getValue(),
                    path.getParent().getLastElement().getValue(),
                    path.getLastElement().getValue()})
            .build();

    static final AttributeDefinition HANDLER = new SimpleAttributeDefinitionBuilder(Constants.HANDLER, ModelType.STRING)
            .setRequired(true)
            .setValidator(new StringLengthValidator(1))
            .setCapabilityReference(Capabilities.CAPABILITY_HANDLER)
            .setRestartAllServices()
            .build();
    private static final List<? extends PersistentResourceDefinition> CHILDREN = Collections.unmodifiableList(Arrays.asList(FilterRefDefinition.INSTANCE));
    static final LocationDefinition INSTANCE = new LocationDefinition();


    private LocationDefinition() {
        super(UndertowExtension.PATH_LOCATION,
                UndertowExtension.getResolver(Constants.HOST, Constants.LOCATION),
                LocationAdd.INSTANCE,
                new ServiceRemoveStepHandler(LocationAdd.INSTANCE) {

                    @Override
                    protected ServiceName serviceName(String name, PathAddress address) {
                        return LOCATION_CAPABILITY.getCapabilityServiceName(address);
                    }
                }
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.singleton(HANDLER);
    }

    @Override
    public List<? extends PersistentResourceDefinition> getChildren() {
        return CHILDREN;
    }

    @Override
    public void registerCapabilities(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerCapability(LOCATION_CAPABILITY);
    }
}
