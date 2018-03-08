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

package org.wildfly.extension.undertow.filters;

import java.util.Arrays;
import java.util.Collection;

import io.undertow.predicate.Predicate;
import io.undertow.predicate.PredicateParser;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.undertow.Capabilities;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.FilterLocation;
import org.wildfly.extension.undertow.PredicateValidator;
import org.wildfly.extension.undertow.UndertowExtension;
import org.wildfly.extension.undertow.UndertowService;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
public class FilterRefDefinition extends PersistentResourceDefinition {

    public static final AttributeDefinition PREDICATE = new SimpleAttributeDefinitionBuilder("predicate", ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setRestartAllServices()
            .setValidator(PredicateValidator.INSTANCE)
            .build();
    public static final AttributeDefinition PRIORITY = new SimpleAttributeDefinitionBuilder("priority", ModelType.INT)
            .setRequired(false)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(1))
            .setValidator(new IntRangeValidator(1, true, true))
            .setRestartAllServices()
            .build();

    public static final FilterRefDefinition INSTANCE = new FilterRefDefinition();


    private FilterRefDefinition() {
        super(UndertowExtension.PATH_FILTER_REF,
                UndertowExtension.getResolver(Constants.FILTER_REF),
                new FilterRefAdd(),
                new ServiceRemoveStepHandler(new FilterRefAdd()) {
                    @Override
                    protected ServiceName serviceName(String name, PathAddress address) {
                        return UndertowService.getFilterRefServiceName(address, name);
                    }
                });
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(PREDICATE, PRIORITY);
    }


    static class FilterRefAdd extends AbstractAddStepHandler {
        FilterRefAdd() {
            super(FilterRefDefinition.PREDICATE, PRIORITY);
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            final PathAddress address = context.getCurrentAddress();
            final String name = context.getCurrentAddressValue();

            final String locationType = address.getElement(address.size() - 2).getKey();

            ServiceName locationService;

            if(locationType.equals(Constants.HOST)) {
                final PathAddress hostAddress = address.getParent();
                final PathAddress serverAddress = hostAddress.getParent();
                final String serverName = serverAddress.getLastElement().getValue();
                final String hostName = hostAddress.getLastElement().getValue();
                locationService = context.getCapabilityServiceName(Capabilities.CAPABILITY_HOST, FilterLocation.class, serverName, hostName);
            } else {
                final PathAddress locationAddress = address.getParent();
                final PathAddress hostAddress = locationAddress.getParent();
                final PathAddress serverAddress = hostAddress.getParent();
                final String locationName = locationAddress.getLastElement().getValue();
                final String serverName = serverAddress.getLastElement().getValue();
                final String hostName = hostAddress.getLastElement().getValue();
                locationService = context.getCapabilityServiceName(Capabilities.CAPABILITY_LOCATION, FilterLocation.class, serverName, hostName, locationName);
            }

            Predicate predicate = null;
            if (model.hasDefined(PREDICATE.getName())) {
                String predicateString = model.get(PREDICATE.getName()).asString();
                predicate = PredicateParser.parse(predicateString, getClass().getClassLoader());
            }

            int priority = PRIORITY.resolveModelAttribute(context, operation).asInt();
            final FilterRef service = new FilterRef(predicate, priority);
            final ServiceTarget target = context.getServiceTarget();
            target.addService(UndertowService.getFilterRefServiceName(address, name), service)
                    .addDependency(UndertowService.FILTER.append(name), FilterService.class, service.getFilter())
                    .addDependency(locationService, FilterLocation.class, service.getLocation())
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();
        }
    }
}
