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

import org.jboss.as.connector.services.bootstrap.BootStrapContextService;
import org.jboss.as.connector.services.bootstrap.NamedBootstrapContext;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.txn.integration.JBossContextXATerminator;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.core.api.bootstrap.CloneableBootstrapContext;
import org.jboss.jca.core.api.workmanager.WorkManager;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="jesper.pedersen@jboss.org">Jesper Pedersen</a>
 * @author <a href="stefano.maestri@redhat.com">Stefano Maestri</a>
 */
public class BootstrapContextAdd extends AbstractAddStepHandler {

    public static final BootstrapContextAdd INSTANCE = new BootstrapContextAdd();

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        for (JcaBootstrapContextDefinition.BootstrapCtxParameters parameter : JcaBootstrapContextDefinition.BootstrapCtxParameters.values()) {
            parameter.getAttribute().validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {

        String name = JcaBootstrapContextDefinition.BootstrapCtxParameters.NAME.getAttribute().resolveModelAttribute(context, model).asString();
        String workmanager = JcaBootstrapContextDefinition.BootstrapCtxParameters.WORKMANAGER.getAttribute().resolveModelAttribute(context, model).asString();
        boolean usingDefaultWm = false;
        CloneableBootstrapContext ctx;
        if (DEFAULT_NAME.equals(workmanager)) {
            usingDefaultWm = true;
            ctx = new NamedBootstrapContext(name);
        } else {
            ctx = new NamedBootstrapContext(name, workmanager);
        }

        ServiceTarget serviceTarget = context.getServiceTarget();

                final BootStrapContextService bootCtxService = new BootStrapContextService(ctx, name, usingDefaultWm);
                serviceTarget
                        .addService(ConnectorServices.BOOTSTRAP_CONTEXT_SERVICE.append(name), bootCtxService)
                        .addDependency(ConnectorServices.WORKMANAGER_SERVICE.append(workmanager), WorkManager.class, bootCtxService.getWorkManagerValueInjector())
                        .addDependency(TxnServices.JBOSS_TXN_CONTEXT_XA_TERMINATOR, JBossContextXATerminator.class, bootCtxService.getXaTerminatorInjector())
                        .addDependency(TxnServices.JBOSS_TXN_ARJUNA_TRANSACTION_MANAGER, com.arjuna.ats.jbossatx.jta.TransactionManagerService.class, bootCtxService.getTxManagerInjector())
                        .addDependency(ConnectorServices.CONNECTOR_CONFIG_SERVICE, JcaSubsystemConfiguration.class, bootCtxService.getJcaConfigInjector())
                        .setInitialMode(ServiceController.Mode.ACTIVE)
                        .install();

    }
}
