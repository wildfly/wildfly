/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.datasources;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.api.management.DataSource;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.msc.service.ServiceController;

/**
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jeff.zhang@jboss.org">Jeff Zhang</a>
 */
public class EnlistmentTraceAttributeWriteHandler extends AbstractWriteAttributeHandler<List<DataSource>> {


    protected EnlistmentTraceAttributeWriteHandler() {
        super(Constants.ENLISTMENT_TRACE);

    }


    @Override
    protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation,
                                           final String parameterName, final ModelNode newValue,
                                           final ModelNode currentValue, final HandbackHolder<List<DataSource>> handbackHolder) throws OperationFailedException {

        final String jndiName = context.readResource(PathAddress.EMPTY_ADDRESS).getModel()
                .get(org.jboss.as.connector.subsystems.common.jndi.Constants.JNDI_NAME.getName()).asString();



        final ServiceController<?> managementRepoService = context.getServiceRegistry(false).getService(
                ConnectorServices.MANAGEMENT_REPOSITORY_SERVICE);
        Boolean boolValue = Constants.ENLISTMENT_TRACE.resolveValue(context, newValue).asBoolean();
        try {
            final ManagementRepository repository = (ManagementRepository) managementRepoService.getValue();
            if (repository.getDataSources() != null) {
                for (DataSource dataSource : repository.getDataSources()) {
                    if (jndiName.equalsIgnoreCase(dataSource.getJndiName())) {
                        dataSource.setEnlistmentTrace(boolValue);
                    }
                }
                List<DataSource> list = new ArrayList<>();
                for (DataSource ds : repository.getDataSources()) {
                    if (jndiName.equalsIgnoreCase(ds.getJndiName())) {
                        list.add(ds);
                    }
                }
                handbackHolder.setHandback(list);
            }

        } catch (Exception e) {
            throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.failedToSetAttribute(e.getLocalizedMessage()));
        }

        return false;

    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String parameterName,
                                         ModelNode valueToRestore, ModelNode valueToRevert,
                                         List<DataSource> handback) throws OperationFailedException {
        Boolean value = Constants.ENLISTMENT_TRACE.resolveValue(context, valueToRestore).asBoolean();
        if (handback != null) {
            for (DataSource ds : handback) {
                ds.setEnlistmentTrace(value);
            }
        }
    }


}



