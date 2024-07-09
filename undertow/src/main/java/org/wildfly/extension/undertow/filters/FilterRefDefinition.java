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
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ServiceRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
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

    public FilterRefDefinition() {
        super(new SimpleResourceDefinition.Parameters(PATH_ELEMENT, UndertowExtension.getResolver(PATH_ELEMENT.getKey()))
                .setAddHandler(new FilterRefAdd())
                .setRemoveHandler(new ServiceRemoveStepHandler(new FilterRefAdd()) {
                    @Override
                    protected ServiceName serviceName(String name, PathAddress address) {
                        return UndertowService.getFilterRefServiceName(address, name);
                    }
                })
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    static class FilterRefAdd extends AbstractAddStepHandler {

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            final PathAddress address = context.getCurrentAddress();
            final String name = context.getCurrentAddressValue();
            final String locationType = address.getElement(address.size() - 2).getKey();
            final ServiceName locationSN;

            if (locationType.equals(Constants.HOST)) {
                final PathAddress hostAddress = address.getParent();
                final PathAddress serverAddress = hostAddress.getParent();
                final String serverName = serverAddress.getLastElement().getValue();
                final String hostName = hostAddress.getLastElement().getValue();
                locationSN = context.getCapabilityServiceName(Host.SERVICE_DESCRIPTOR, serverName, hostName);
            } else {
                final PathAddress locationAddress = address.getParent();
                final PathAddress hostAddress = locationAddress.getParent();
                final PathAddress serverAddress = hostAddress.getParent();
                final String locationName = locationAddress.getLastElement().getValue();
                final String serverName = serverAddress.getLastElement().getValue();
                final String hostName = hostAddress.getLastElement().getValue();
                locationSN = context.getCapabilityServiceName(Capabilities.CAPABILITY_LOCATION, FilterLocation.class, serverName, hostName, locationName);
            }

            Predicate predicate = null;
            if (model.hasDefined(PREDICATE.getName())) {
                String predicateString = PREDICATE.resolveModelAttribute(context, model).asString();
                predicate = PredicateParser.parse(predicateString, getClass().getClassLoader());
            }

            int priority = PRIORITY.resolveModelAttribute(context, operation).asInt();
            final ServiceTarget target = context.getServiceTarget();
            final ServiceName sn = UndertowService.getFilterRefServiceName(address, name);
            final ServiceBuilder<?> sb = target.addService(sn);
            final Consumer<UndertowFilter> frConsumer = sb.provides(sn);
            final Supplier<PredicateHandlerWrapper> fSupplier = sb.requires(UndertowService.FILTER.append(name));
            final Supplier<FilterLocation> lSupplier = sb.requires(locationSN);
            sb.setInstance(new FilterService(frConsumer, fSupplier, lSupplier, predicate, priority));
            sb.install();
        }
    }
}
