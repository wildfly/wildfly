/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.resourceadapters;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.connector.util.CopyOnWriteArrayListMultiMap;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import java.io.File;

import static org.jboss.as.connector.subsystems.resourceadapters.Constants.*;

/**
 * Handler for adding the resource adapters subsystem.
 *
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 * @author John Bailey
 */
class ResourceAdaptersSubsystemAdd extends AbstractAddStepHandler {

    static final ResourceAdaptersSubsystemAdd INSTANCE = new ResourceAdaptersSubsystemAdd();

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        Constants.REPORT_DIRECTORY.validateAndSet(operation, model);
    }


    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        final Resource subsystemResource = context.readResourceFromRoot(PathAddress.pathAddress(ResourceAdaptersExtension.SUBSYSTEM_PATH));
        final ConfiguredAdaptersService configuredAdaptersService = new ConfiguredAdaptersService();
        final CopyOnWriteArrayListMultiMap<String, ServiceName> value = configuredAdaptersService.getValue();
        for (Resource.ResourceEntry re : subsystemResource.getChildren(RESOURCEADAPTER_NAME)) {
            value.putIfAbsent(re.getModel().get(ARCHIVE.getName()).asString(), ConnectorServices.RA_SERVICE.append(re.getName()));
        }
        final ServiceBuilder<?> configuredAdaptersServiceBuilder = context.getServiceTarget().addService(ConnectorServices.RESOURCEADAPTERS_CONFIGURED_ADAPTERS_SERVICE, configuredAdaptersService);
        configuredAdaptersServiceBuilder.setInitialMode(ServiceController.Mode.ACTIVE).install();

        final String reportDirectoryName = Constants.REPORT_DIRECTORY.resolveModelAttribute(context, operation).asString();
        final File reportDirectory = new File(reportDirectoryName);
        if (!reportDirectory.exists()) {
            throw ConnectorLogger.SUBSYSTEM_RA_LOGGER.reportDirectoryDoesNotExist(reportDirectoryName);
        }
        ReportDirectoryService reportDirectoryService = new ReportDirectoryService(reportDirectory);
        final ServiceBuilder<?> reportDirectoryServiceBuilder = context.getServiceTarget().addService(ConnectorServices.RESOURCEADAPTERS_REPORT_DIRECTORY_SERVICE, reportDirectoryService);
        reportDirectoryServiceBuilder.setInitialMode(ServiceController.Mode.ACTIVE).install();
    }
}
