/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

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
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.undertow.Constants;
import org.wildfly.extension.undertow.UndertowExtension;
import org.wildfly.extension.undertow.UndertowService;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
public class FilterRefDefinition extends PersistentResourceDefinition {

    public static final AttributeDefinition PREDICATE = new SimpleAttributeDefinitionBuilder("predicate", ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();
    public static final AttributeDefinition PRIORITY = new SimpleAttributeDefinitionBuilder("priority", ModelType.INT)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(1))
            .setValidator(new IntRangeValidator(1, true, true))
            .build();

    public static final FilterRefDefinition INSTANCE = new FilterRefDefinition();


    private FilterRefDefinition() {
        super(UndertowExtension.PATH_FILTER_REF,
                UndertowExtension.getResolver(Constants.FILTER_REF),
                new FilterRefAdd(),
                ReloadRequiredRemoveStepHandler.INSTANCE);
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
            final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
            final String name = address.getLastElement().getValue();

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
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();
        }
    }
}
