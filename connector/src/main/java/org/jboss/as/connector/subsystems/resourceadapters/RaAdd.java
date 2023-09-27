/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.subsystems.jca.Constants.DEFAULT_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ARCHIVE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.MODULE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.STATISTICS_ENABLED;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.services.resourceadapters.statistics.ResourceAdapterStatisticsService;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.api.metadata.resourceadapter.Activation;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Operation handler responsible for adding a Ra.
 *
 * @author maeste
 */
public class RaAdd extends AbstractAddStepHandler {
    static final RaAdd INSTANCE = new RaAdd();

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (final AttributeDefinition attribute : CommonAttributes.RESOURCE_ADAPTER_ATTRIBUTE) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    public void performRuntime(final OperationContext context, ModelNode operation, final Resource resource) throws OperationFailedException {
        final ModelNode model = resource.getModel();

        // Compensating is remove
        final String name = context.getCurrentAddressValue();
        final String archiveOrModuleName;
        final boolean statsEnabled = STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();

        if (!model.hasDefined(ARCHIVE.getName()) && !model.hasDefined(MODULE.getName())) {
            throw ConnectorLogger.ROOT_LOGGER.archiveOrModuleRequired();
        }
        if (model.get(ARCHIVE.getName()).isDefined()) {
            archiveOrModuleName = model.get(ARCHIVE.getName()).asString();
        } else {
            archiveOrModuleName = model.get(MODULE.getName()).asString();
        }


        ModifiableResourceAdapter resourceAdapter = RaOperationUtil.buildResourceAdaptersObject(name, context, operation, archiveOrModuleName);
        List<ServiceController<?>> newControllers = new ArrayList<ServiceController<?>>();
        if (model.get(ARCHIVE.getName()).isDefined()) {
            RaOperationUtil.installRaServices(context, name, resourceAdapter, newControllers);
        } else {
            RaOperationUtil.installRaServicesAndDeployFromModule(context, name, resourceAdapter, archiveOrModuleName, newControllers);
            if (context.isBooting()) {
                context.addStep(new OperationStepHandler() {
                    public void execute(final OperationContext context, ModelNode operation) throws OperationFailedException {

                        //Next lines activate configuration on module deployed rar
                        //in case there is 2 different resource-adapter config using same module deployed rar
                        // a Deployment sercivice could be already present and need a restart to consider also this
                        //newly added configuration
                        ServiceName restartedServiceName = RaOperationUtil.restartIfPresent(context, archiveOrModuleName, name);
                        if (restartedServiceName == null) {
                            RaOperationUtil.activate(context, name, archiveOrModuleName);
                        }
                        context.completeStep(new OperationContext.RollbackHandler() {
                            @Override
                            public void handleRollback(OperationContext context, ModelNode operation) {
                                try {
                                    RaOperationUtil.removeIfActive(context, archiveOrModuleName, name);
                                } catch (OperationFailedException e) {

                                }

                            }
                        });
                    }
                }, OperationContext.Stage.RUNTIME);
            }
        }
        ServiceRegistry registry = context.getServiceRegistry(true);

        final ServiceController<?> RaxmlController = registry.getService(ServiceName.of(ConnectorServices.RA_SERVICE, name));
        Activation raxml = (Activation) RaxmlController.getValue();
        ServiceName serviceName = ConnectorServices.getDeploymentServiceName(archiveOrModuleName, name);
        String bootStrapCtxName = DEFAULT_NAME;
        if (raxml.getBootstrapContext() != null && !raxml.getBootstrapContext().equals("undefined")) {
            bootStrapCtxName = raxml.getBootstrapContext();
        }

        ResourceAdapterStatisticsService raStatsService = new ResourceAdapterStatisticsService(context.getResourceRegistrationForUpdate(), name, statsEnabled);

        ServiceBuilder statsServiceBuilder = context.getServiceTarget().addService(ServiceName.of(ConnectorServices.RA_SERVICE, name).append(ConnectorServices.STATISTICS_SUFFIX), raStatsService);
        statsServiceBuilder.addDependency(ConnectorServices.BOOTSTRAP_CONTEXT_SERVICE.append(bootStrapCtxName), Object.class, raStatsService.getBootstrapContextInjector())
                .addDependency(serviceName, Object.class, raStatsService.getResourceAdapterDeploymentInjector())
                .setInitialMode(ServiceController.Mode.PASSIVE)
                .install();

        PathElement peStats = PathElement.pathElement(Constants.STATISTICS_NAME, "extended");

        final Resource statsResource = new IronJacamarResource.IronJacamarRuntimeResource();

        resource.registerChild(peStats, statsResource);

    }
}
