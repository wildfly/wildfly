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

package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * Operation handler responsible for removing a (XA)DataSource.
 *
 * @author Stefano Maestrioperation2.get(OP).set("write-attribute");
 */
public class XaDataSourcePropertyRemove extends AbstractRemoveStepHandler {

    public static final XaDataSourcePropertyRemove INSTANCE = new XaDataSourcePropertyRemove();


    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {


        if (context.isResourceServiceRestartAllowed()) {
                final ModelNode address = operation.require(OP_ADDR);
                final PathAddress path = PathAddress.pathAddress(address);
                final String jndiName = path.getElement(path.size() - 2).getValue();
                final String configPropertyName = PathAddress.pathAddress(address).getLastElement().getValue();

                ServiceName configPropertyServiceName = XADataSourceConfigService.SERVICE_NAME_BASE.append(jndiName).
                        append("xa-datasource-properties").append(configPropertyName);

                context.removeService(configPropertyServiceName);

            } else {
                context.reloadRequired();
            }

        }
}
