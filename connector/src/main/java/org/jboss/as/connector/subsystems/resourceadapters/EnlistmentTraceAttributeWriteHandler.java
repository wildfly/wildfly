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

package org.jboss.as.connector.subsystems.resourceadapters;

import java.util.LinkedList;
import java.util.List;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.api.management.ConnectionManager;
import org.jboss.jca.core.api.management.Connector;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.msc.service.ServiceController;

/**
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jeff.zhang@jboss.org">Jeff Zhang</a>
 */
public class EnlistmentTraceAttributeWriteHandler extends AbstractWriteAttributeHandler<List<ConnectionManager>> {


    protected EnlistmentTraceAttributeWriteHandler() {
        super(Constants.ENLISTMENT_TRACE);
    }


    @Override
    protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation,
                                           final String parameterName, final ModelNode newValue,
                                           final ModelNode currentValue, final HandbackHolder<List<ConnectionManager>> handbackHolder) throws OperationFailedException {
        final String jndiName = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().get(Constants.JNDINAME.getName()).asString();

        final ServiceController<?> managementRepoService = context.getServiceRegistry(false).getService(
                ConnectorServices.MANAGEMENT_REPOSITORY_SERVICE);
        Boolean boolValue = Constants.ENLISTMENT_TRACE.resolveValue(context, newValue).asBoolean();
        try {
            final ManagementRepository repository = (ManagementRepository) managementRepoService.getValue();
            if (repository.getConnectors() != null) {
                List<ConnectionManager> handback = new LinkedList<>();
                for (Connector connector : repository.getConnectors()) {
                    for (ConnectionManager cm : connector.getConnectionManagers()) {
                        if (jndiName.equalsIgnoreCase(cm.getUniqueId())) {
                            cm.setEnlistmentTrace(boolValue);
                            handback.add(cm);
                        }
                    }
                }
                handbackHolder.setHandback(handback);

            }

        } catch (Exception e) {
            throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.failedToSetAttribute(e.getLocalizedMessage()));
        }
        return false;

    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String parameterName,
                                         ModelNode valueToRestore, ModelNode valueToRevert,
                                         List<ConnectionManager> handback) throws OperationFailedException {

        Boolean value = Constants.ENLISTMENT_TRACE.resolveValue(context, valueToRestore).asBoolean();
        if (handback != null) {
            for (ConnectionManager ds : handback) {
                ds.setEnlistmentTrace(value);
            }
        }
    }



}



