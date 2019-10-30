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
package org.jboss.as.connector.subsystems.jca;

import static org.jboss.as.connector.subsystems.jca.Constants.WORKMANAGER_LONG_RUNNING;
import static org.jboss.as.connector.subsystems.jca.Constants.WORKMANAGER_SHORT_RUNNING;

import java.util.concurrent.Executor;

import org.jboss.as.connector.services.workmanager.NamedWorkManager;
import org.jboss.as.connector.services.workmanager.WorkManagerService;
import org.jboss.as.connector.services.workmanager.statistics.WorkManagerStatisticsService;
import org.jboss.as.connector.subsystems.resourceadapters.IronJacamarResource;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.as.txn.integration.JBossContextXATerminator;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.api.workmanager.WorkManager;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @author <a href="stefano.maestri@redhat.com">Stefano Maestri</a>
 */
public class WorkManagerAdd extends AbstractAddStepHandler {

    public static final WorkManagerAdd INSTANCE = new WorkManagerAdd();


    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        for (JcaWorkManagerDefinition.WmParameters parameter : JcaWorkManagerDefinition.WmParameters.values()) {
            parameter.getAttribute().validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final Resource resource) throws OperationFailedException {

        String name = JcaWorkManagerDefinition.WmParameters.NAME.getAttribute().resolveModelAttribute(context, resource.getModel()).asString();
        boolean elytronEnabled = JcaWorkManagerDefinition.WmParameters.ELYTRON_ENABLED.getAttribute().resolveModelAttribute(context, resource.getModel()).asBoolean();

        ServiceTarget serviceTarget = context.getServiceTarget();


        NamedWorkManager wm = new NamedWorkManager(name, elytronEnabled);
        WorkManagerService wmService = new WorkManagerService(wm);
        ServiceBuilder builder = serviceTarget
                .addService(ConnectorServices.WORKMANAGER_SERVICE.append(name), wmService);

        if (resource.hasChild(PathElement.pathElement(Element.LONG_RUNNING_THREADS.getLocalName()))) {
            builder.addDependency(ThreadsServices.EXECUTOR.append(WORKMANAGER_LONG_RUNNING).append(name), Executor.class, wmService.getExecutorLongInjector());
        }
        builder.addDependency(ThreadsServices.EXECUTOR.append(WORKMANAGER_SHORT_RUNNING).append(name), Executor.class, wmService.getExecutorShortInjector());

        builder.addDependency(TxnServices.JBOSS_TXN_CONTEXT_XA_TERMINATOR, JBossContextXATerminator.class, wmService.getXaTerminatorInjector())
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install();


        WorkManagerStatisticsService wmStatsService = new WorkManagerStatisticsService(context.getResourceRegistrationForUpdate(), name, true);
        serviceTarget
                .addService(ConnectorServices.WORKMANAGER_STATS_SERVICE.append(name), wmStatsService)
                .addDependency(ConnectorServices.WORKMANAGER_SERVICE.append(name), WorkManager.class, wmStatsService.getWorkManagerInjector())
                .setInitialMode(ServiceController.Mode.PASSIVE).install();
        PathElement peLocaldWm = PathElement.pathElement(org.jboss.as.connector.subsystems.resourceadapters.Constants.STATISTICS_NAME, "local");

        final Resource wmResource = new IronJacamarResource.IronJacamarRuntimeResource();

        if (!resource.hasChild(peLocaldWm))
            resource.registerChild(peLocaldWm, wmResource);

    }
}
