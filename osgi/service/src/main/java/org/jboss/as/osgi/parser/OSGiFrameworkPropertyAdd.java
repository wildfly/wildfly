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
package org.jboss.as.osgi.parser;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * @author David Bosschaert
 * @author Thomas.Diesler@jboss.com
 */
public class OSGiFrameworkPropertyAdd extends AbstractAddStepHandler {
    static final OSGiFrameworkPropertyAdd INSTANCE = new OSGiFrameworkPropertyAdd();

    private OSGiFrameworkPropertyAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        FrameworkPropertyResource.VALUE.validateAndSet(operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler,
            List<ServiceController<?>> newControllers) throws OperationFailedException {

        String propName = operation.get(ModelDescriptionConstants.OP_ADDR).asObject().get(ModelConstants.PROPERTY).asString();
        String propValue = FrameworkPropertyResource.VALUE.resolveModelAttribute(context, model).asString();

        SubsystemState subsystemState = SubsystemState.getSubsystemState(context);
        if (subsystemState != null) {
            subsystemState.setProperty(propName, propValue);
        }
    }

    @Override
    protected void rollbackRuntime(OperationContext context, ModelNode operation, ModelNode model, List<ServiceController<?>> controllers) {
        String propName = operation.get(ModelDescriptionConstants.OP_ADDR).asObject().get(ModelConstants.PROPERTY).asString();
        SubsystemState subsystemState = SubsystemState.getSubsystemState(context);
        if (subsystemState != null) {
            subsystemState.setProperty(propName, null);
        }
    }

}
