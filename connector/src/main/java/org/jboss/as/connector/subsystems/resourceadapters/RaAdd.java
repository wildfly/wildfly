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

package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.logging.ConnectorLogger.SUBSYSTEM_RA_LOGGER;
import static org.jboss.as.connector.subsystems.jca.Constants.DEFAULT_NAME;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ARCHIVE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.AUTHENTICATION_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.AUTHENTICATION_CONTEXT_AND_APPLICATION;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.ELYTRON_ENABLED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.MODULE;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_AUTHENTICATION_CONTEXT;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_ELYTRON_ENABLED;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RECOVERY_SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.SECURITY_DOMAIN_AND_APPLICATION;
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
        // add extra security validation: authentication contexts should only be defined when Elytron Enabled is true
        // domains/application attributes should only be defined when Elytron enabled is undefined or false (default value)
        if (ELYTRON_ENABLED.resolveModelAttribute(context, model).asBoolean()) {
            if (model.hasDefined(SECURITY_DOMAIN.getName()))
                throw SUBSYSTEM_RA_LOGGER.attributeRequiresFalseOrUndefinedAttribute(SECURITY_DOMAIN.getName(), ELYTRON_ENABLED.getName());
            else if (model.hasDefined(SECURITY_DOMAIN_AND_APPLICATION.getName()))
                throw SUBSYSTEM_RA_LOGGER.attributeRequiresFalseOrUndefinedAttribute(SECURITY_DOMAIN_AND_APPLICATION.getName(), ELYTRON_ENABLED.getName());
            else if (model.hasDefined(APPLICATION.getName()))
                throw SUBSYSTEM_RA_LOGGER.attributeRequiresFalseOrUndefinedAttribute(APPLICATION.getName(), ELYTRON_ENABLED.getName());
        } else {
            if (model.hasDefined(AUTHENTICATION_CONTEXT.getName()))
                throw SUBSYSTEM_RA_LOGGER.attributeRequiresTrueAttribute(AUTHENTICATION_CONTEXT.getName(), ELYTRON_ENABLED.getName());
            else if (model.hasDefined(AUTHENTICATION_CONTEXT_AND_APPLICATION.getName()))
                throw SUBSYSTEM_RA_LOGGER.attributeRequiresTrueAttribute(AUTHENTICATION_CONTEXT_AND_APPLICATION.getName(), ELYTRON_ENABLED.getName());
        }
        // do the same for recovery security attributes
        if (RECOVERY_ELYTRON_ENABLED.resolveModelAttribute(context, model).asBoolean()) {
            if (model.hasDefined(RECOVERY_SECURITY_DOMAIN.getName()))
                throw SUBSYSTEM_RA_LOGGER.attributeRequiresFalseOrUndefinedAttribute(RECOVERY_SECURITY_DOMAIN.getName(), RECOVERY_ELYTRON_ENABLED.getName());
        } else {
            if (model.hasDefined(RECOVERY_AUTHENTICATION_CONTEXT.getName()))
                throw SUBSYSTEM_RA_LOGGER.attributeRequiresTrueAttribute(RECOVERY_AUTHENTICATION_CONTEXT.getName(), RECOVERY_ELYTRON_ENABLED.getName());
        }

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
        statsServiceBuilder.addDependency(ConnectorServices.BOOTSTRAP_CONTEXT_SERVICE.append(bootStrapCtxName), raStatsService.getBootstrapContextInjector())
                .addDependency(serviceName, raStatsService.getResourceAdapterDeploymentInjector())
                .setInitialMode(ServiceController.Mode.PASSIVE)
                .install();

        PathElement peStats = PathElement.pathElement(Constants.STATISTICS_NAME, "extended");

        final Resource statsResource = new IronJacamarResource.IronJacamarRuntimeResource();

        resource.registerChild(peStats, statsResource);

    }
}
