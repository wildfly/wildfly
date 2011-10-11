/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.logging;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;

import static org.jboss.as.logging.CommonAttributes.CLASS;
import static org.jboss.as.logging.CommonAttributes.MODULE;
import static org.jboss.as.logging.CommonAttributes.PROPERTIES;

/**
 * Date: 03.08.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class CustomHandlerAdd extends HandlerAddProperties<CustomHandlerService> {

    static final CustomHandlerAdd INSTANCE = new CustomHandlerAdd();

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        super.populateModel(operation, model);
        MODULE.validateAndSet(operation, model);
        CLASS.validateAndSet(operation, model);
        copy(PROPERTIES, operation, model);
    }

    @Override
    protected CustomHandlerService createHandlerService(final ModelNode model) throws OperationFailedException {
        final String className = CLASS.validateResolvedOperation(model).asString();
        final String moduleName = MODULE.validateResolvedOperation(model).asString();
        return new CustomHandlerService(className, moduleName);
    }

    @Override
    protected void updateRuntime(final OperationContext context, final ServiceBuilder<?> serviceBuilder, final String name, final CustomHandlerService service, final ModelNode model) throws OperationFailedException {
        final ModelNode properties = model.get(PROPERTIES);
        if (properties.isDefined()) {
            service.addProperties(properties.asPropertyList());
        }
    }


}
