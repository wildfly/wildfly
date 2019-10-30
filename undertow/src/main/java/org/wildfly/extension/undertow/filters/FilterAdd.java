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

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.undertow.Handler;
import org.wildfly.extension.undertow.UndertowService;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
class FilterAdd extends AbstractAddStepHandler {

    private Handler handler;

    FilterAdd(Handler handler) {
        super(handler.getAttributes());
        this.handler = handler;
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String name = context.getCurrentAddressValue();

        final FilterService service = new FilterService(handler, getResolvedModel(context, model));
        final ServiceTarget target = context.getServiceTarget();
        target.addService(UndertowService.FILTER.append(name), service)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install();
    }

    private ModelNode getResolvedModel(OperationContext context, ModelNode model) throws OperationFailedException {
        ModelNode resolved = new ModelNode();
        for (AttributeDefinition attribute : attributes) {
            resolved.get(attribute.getName()).set(attribute.resolveModelAttribute(context, model));
        }
        return resolved;
    }

}
