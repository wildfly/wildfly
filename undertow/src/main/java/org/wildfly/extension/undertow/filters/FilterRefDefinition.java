/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.filters;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.undertow.predicate.Predicate;
import io.undertow.predicate.PredicateParser;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityReferenceRecorder;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.undertow.Capabilities;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.FilterLocation;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.PredicateValidator;
import org.wildfly.extension.undertow.UndertowExtension;
import org.wildfly.extension.undertow.UndertowFilter;
import org.wildfly.extension.undertow.UndertowService;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class FilterRefDefinition extends PersistentResourceDefinition {
    public static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.FILTER_REF);
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

    public static final Collection<AttributeDefinition> ATTRIBUTES = List.of(PREDICATE, PRIORITY);

    /**
     * Creates a new FilterRefDefinition
     * @param forHost {@code true} if the definition is for a filter-ref that is a direct child of a host resource;
     *                {@code false} if it is a child of the location child resource
     */
    public FilterRefDefinition(boolean forHost) {
        super(filterRefParameters(forHost));
    }

    private static SimpleResourceDefinition.Parameters filterRefParameters(boolean forHost) {

        FilterCapabilities capability = forHost
                ? FilterCapabilities.FILTER_HOST_REF_CAPABILITY
                : FilterCapabilities.FILTER_LOCATION_REF_CAPABILITY;
        return new SimpleResourceDefinition.Parameters(PATH_ELEMENT, UndertowExtension.getResolver(PATH_ELEMENT.getKey()))
                .setAddHandler(new FilterRefAdd(forHost))
                .setRemoveHandler(new ServiceRemoveStepHandler(new FilterRefAdd(forHost)) {
                    @Override
                    protected ServiceName serviceName(String name, PathAddress address) {
                        return UndertowService.getFilterRefServiceName(address, name);
                    }
                })
                .addCapabilities(capability.getDefinition())
                // TODO resolve problem generating a feature spec when this is used
                //.addRequirement(capability.getName(), capability.getDynamicNameMapper(),
                //        FilterCapabilities.FILTER_CAPABILITY.getName(), FilterCapabilities.FILTER_CAPABILITY.getDynamicNameMapper())
                ;
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    static class FilterRefAdd extends AbstractAddStepHandler {

        private final boolean forHost;

        private FilterRefAdd(boolean forHost) {
            this.forHost = forHost;
        }

        // TODO remove this when registering via SimpleResourceDefinition.Parameters.addRequirement works
        @Override
        protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
            super.recordCapabilitiesAndRequirements(context, operation, resource);

            FilterCapabilities capability = forHost
                    ? FilterCapabilities.FILTER_HOST_REF_CAPABILITY
                    : FilterCapabilities.FILTER_LOCATION_REF_CAPABILITY;
            CapabilityReferenceRecorder filterRefRecorder =
                    new CapabilityReferenceRecorder.ResourceCapabilityReferenceRecorder(
                            capability.getDynamicNameMapper(),
                            capability.getName(),
                            FilterCapabilities.FILTER_CAPABILITY.getDynamicNameMapper(),
                            FilterCapabilities.FILTER_CAPABILITY.getName());
            filterRefRecorder.addCapabilityRequirements(context, resource, null);
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            final PathAddress address = context.getCurrentAddress();
            final String name = context.getCurrentAddressValue();
            final ServiceName locationSN;
            final FilterCapabilities capabilityType;

            if (forHost) {
                final PathAddress hostAddress = address.getParent();
                final PathAddress serverAddress = hostAddress.getParent();
                final String serverName = serverAddress.getLastElement().getValue();
                final String hostName = hostAddress.getLastElement().getValue();
                locationSN = context.getCapabilityServiceName(Host.SERVICE_DESCRIPTOR, serverName, hostName);
                capabilityType = FilterCapabilities.FILTER_HOST_REF_CAPABILITY;
            } else {
                final PathAddress locationAddress = address.getParent();
                final PathAddress hostAddress = locationAddress.getParent();
                final PathAddress serverAddress = hostAddress.getParent();
                final String locationName = locationAddress.getLastElement().getValue();
                final String serverName = serverAddress.getLastElement().getValue();
                final String hostName = hostAddress.getLastElement().getValue();
                locationSN = context.getCapabilityServiceName(Capabilities.CAPABILITY_LOCATION, FilterLocation.class, serverName, hostName, locationName);
                capabilityType = FilterCapabilities.FILTER_LOCATION_REF_CAPABILITY;
            }

            Predicate predicate = null;
            if (model.hasDefined(PREDICATE.getName())) {
                String predicateString = PREDICATE.resolveModelAttribute(context, model).asString();
                predicate = PredicateParser.parse(predicateString, getClass().getClassLoader());
            }

            final int priority = PRIORITY.resolveModelAttribute(context, operation).asInt();
            final CapabilityServiceTarget target = context.getCapabilityServiceTarget();
            final ServiceName sn = UndertowService.getFilterRefServiceName(address, name);
            final CapabilityServiceBuilder<?> csb = target.addCapability(capabilityType.getDefinition());
            final Consumer<UndertowFilter> frConsumer = csb.provides(capabilityType.getDefinition() , sn);
            final Supplier<PredicateHandlerWrapper> fSupplier = csb.requiresCapability(Capabilities.CAPABILITY_FILTER, PredicateHandlerWrapper.class, name);
            final Supplier<FilterLocation> lSupplier = csb.requires(locationSN);
            csb.setInitialMode(Mode.ACTIVE);
            csb.setInstance(new FilterService(frConsumer, fSupplier, lSupplier, predicate, priority));
            csb.install();
        }
    }
}