/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.operations.common;

import java.util.Locale;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 * @author Brian Stansberry
 */
public class ManagementSocketAddHandler extends AbstractAddStepHandler implements DescriptionProvider {

    public static final String OPERATION_NAME = "add-management-socket";

    public static final ManagementSocketAddHandler INSTANCE = new ManagementSocketAddHandler();

    protected ManagementSocketAddHandler() {
    }

    protected void populateModel(ModelNode operation, ModelNode model) {
        final String interfaceName = operation.require(ModelDescriptionConstants.INTERFACE).asString();
        final int port = operation.require(ModelDescriptionConstants.PORT).asInt();

        model.get(ModelDescriptionConstants.MANAGEMENT_INTERFACE, ModelDescriptionConstants.INTERFACE).set(interfaceName);
        model.get(ModelDescriptionConstants.MANAGEMENT_INTERFACE, ModelDescriptionConstants.PORT).set(port);
    }

    protected boolean requiresRuntime(OperationContext context) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ModelNode getModelDescription(Locale locale) {
        // TODO Auto-generated method stub
        return new ModelNode();
    }
}
