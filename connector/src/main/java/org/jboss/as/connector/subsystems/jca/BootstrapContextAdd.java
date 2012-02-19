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

import static org.jboss.as.connector.subsystems.jca.Constants.DEFAULT_NAME;

import java.util.List;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.bootstrap.BootStrapContextService;
import org.jboss.as.connector.bootstrap.NamedBootstrapContext;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.jca.core.api.bootstrap.CloneableBootstrapContext;
import org.jboss.jca.core.api.workmanager.WorkManager;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.tm.JBossXATerminator;

/**
 * @author <a href="jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @author <a href="stefano.maestri@redhat.com">Stefano Maestri</a>
 */
public class BootstrapContextAdd extends AbstractBoottimeAddStepHandler {

    public static final BootstrapContextAdd INSTANCE = new BootstrapContextAdd();

    public static enum BootstrapCtxParameters {
        NAME(SimpleAttributeDefinitionBuilder.create("name", ModelType.STRING)
                .setAllowExpression(true)
                .setAllowNull(false)
                .setMeasurementUnit(MeasurementUnit.NONE)
                .setRestartAllServices()
                .setXmlName("name")
                .build()),
        WORKMANAGER(SimpleAttributeDefinitionBuilder.create("workmanager", ModelType.STRING)
                .setAllowExpression(true)
                .setAllowNull(false)
                .setMeasurementUnit(MeasurementUnit.NONE)
                .setRestartAllServices()
                .setXmlName("workmanager")
                .build());


        private BootstrapCtxParameters(SimpleAttributeDefinition attribute) {
            this.attribute = attribute;
        }

        public SimpleAttributeDefinition getAttribute() {
            return attribute;
        }

        private SimpleAttributeDefinition attribute;
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        for (BootstrapCtxParameters parameter : BootstrapCtxParameters.values()) {
            parameter.getAttribute().validateAndSet(operation, model);
        }
    }

    @Override
    protected void performBoottime(final OperationContext context, final ModelNode operation, final ModelNode model,
                                   final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {

        String name = BootstrapCtxParameters.NAME.getAttribute().resolveModelAttribute(context, model).asString();
        String workmanager = BootstrapCtxParameters.WORKMANAGER.getAttribute().resolveModelAttribute(context, model).asString();
        boolean usingDefaultWm = false;
        if (DEFAULT_NAME.equals( workmanager)) {
            usingDefaultWm = true;
        }

        ServiceTarget serviceTarget = context.getServiceTarget();

        CloneableBootstrapContext ctx = new NamedBootstrapContext(name);
                final BootStrapContextService bootCtxService = new BootStrapContextService(ctx, name, usingDefaultWm);
                newControllers.add(serviceTarget
                        .addService(ConnectorServices.BOOTSTRAP_CONTEXT_SERVICE.append(name), bootCtxService)
                        .addDependency(ConnectorServices.WORKMANAGER_SERVICE.append(workmanager), WorkManager.class, bootCtxService.getWorkManagerValueInjector())
                        .addDependency(TxnServices.JBOSS_TXN_XA_TERMINATOR, JBossXATerminator.class, bootCtxService.getXaTerminatorInjector())
                        .addDependency(TxnServices.JBOSS_TXN_ARJUNA_TRANSACTION_MANAGER, com.arjuna.ats.jbossatx.jta.TransactionManagerService.class, bootCtxService.getTxManagerInjector())
                        .addDependency(ConnectorServices.CONNECTOR_CONFIG_SERVICE, JcaSubsystemConfiguration.class, bootCtxService.getJcaConfigInjector())
                        .addListener(verificationHandler)
                        .setInitialMode(ServiceController.Mode.ACTIVE)
                        .install());

    }
}
