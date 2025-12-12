/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.connector.subsystems.resourceadapters;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;

import java.io.File;

public class ReportDirectoryWriteHandler extends AbstractWriteAttributeHandler<Void> {

    private final AttributeDefinition attributeDefinition;

    ReportDirectoryWriteHandler(final AttributeDefinition attributeDefinition) {
        super(attributeDefinition);
        this.attributeDefinition = attributeDefinition;
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                           ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        updateReportDirectory(context, model);

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                         ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        final ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
        restored.get(attributeName).set(valueToRestore);
        updateReportDirectory(context, restored);
    }

    void updateReportDirectory(final OperationContext context, final ModelNode model) throws OperationFailedException {

        final String reportDirectoryName = this.attributeDefinition.resolveModelAttribute(context, model).asString();
        final File reportDirectory = new File(reportDirectoryName);
        if(!reportDirectory.exists()){
            throw ConnectorLogger.SUBSYSTEM_RA_LOGGER.reportDirectoryDoesNotExist(reportDirectoryName);
        }
        final ServiceRegistry registry = context.getServiceRegistry(true);

        final ServiceController<?> ejbNameServiceController = registry.getService(ConnectorServices.RESOURCEADAPTERS_REPORT_DIRECTORY_SERVICE);
        ReportDirectoryService service = (ReportDirectoryService) ejbNameServiceController.getValue();
        service.setReportDirectory(reportDirectory);
    }
}
