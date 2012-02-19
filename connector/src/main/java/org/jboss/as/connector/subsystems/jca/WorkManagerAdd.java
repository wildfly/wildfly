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

import java.util.List;
import java.util.concurrent.Executor;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.workmanager.NamedWorkManager;
import org.jboss.as.connector.workmanager.WorkManagerService;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.jca.core.api.workmanager.WorkManager;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.tm.JBossXATerminator;

/**
 * @author <a href="jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @author <a href="stefano.maestri@redhat.com">Stefano Maestri</a>
 */
public class WorkManagerAdd extends AbstractBoottimeAddStepHandler {

    public static final WorkManagerAdd INSTANCE = new WorkManagerAdd();

    public static enum WmParameters {
        NAME(SimpleAttributeDefinitionBuilder.create("name", ModelType.STRING)
                .setAllowExpression(true)
                .setAllowNull(false)
                .setMeasurementUnit(MeasurementUnit.NONE)
                .setRestartAllServices()
                .setXmlName("name")
                .build());


        private WmParameters(SimpleAttributeDefinition attribute) {
            this.attribute = attribute;
        }

        public SimpleAttributeDefinition getAttribute() {
            return attribute;
        }

        private SimpleAttributeDefinition attribute;
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        for (WmParameters parameter : WmParameters.values()) {
            parameter.getAttribute().validateAndSet(operation, model);
        }
    }

    @Override
    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model,
                                   final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {

        String name = WmParameters.NAME.getAttribute().resolveModelAttribute(context, model).asString();

        ServiceTarget serviceTarget = context.getServiceTarget();

        WorkManager wm = new NamedWorkManager(name);

        final WorkManagerService wmService = new WorkManagerService(wm);
        ServiceBuilder builder = serviceTarget
                .addService(ConnectorServices.WORKMANAGER_SERVICE.append(name), wmService);
        if (operation.get(WORKMANAGER_LONG_RUNNING).isDefined() && operation.get(WORKMANAGER_LONG_RUNNING).asBoolean()) {
            builder.addDependency(ThreadsServices.EXECUTOR.append(name + "-" + WORKMANAGER_LONG_RUNNING), Executor.class, wmService.getExecutorLongInjector());
        }
        builder.addDependency(ThreadsServices.EXECUTOR.append(name + "-" + WORKMANAGER_SHORT_RUNNING), Executor.class, wmService.getExecutorShortInjector());

        builder.addDependency(TxnServices.JBOSS_TXN_XA_TERMINATOR, JBossXATerminator.class, wmService.getXaTerminatorInjector())
                .addListener(verificationHandler)
                .setInitialMode(ServiceController.Mode.ACTIVE)
                .install();
    }
}
