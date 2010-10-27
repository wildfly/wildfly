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

package org.jboss.as.connector;

import java.util.concurrent.Executor;

import org.jboss.as.connector.deployers.RaDeploymentActivator;
import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.as.txn.TxnServices;
import org.jboss.jca.core.api.bootstrap.CloneableBootstrapContext;
import org.jboss.jca.core.api.workmanager.WorkManager;
import org.jboss.jca.core.bootstrapcontext.BaseCloneableBootstrapContext;
import org.jboss.jca.core.workmanager.WorkManagerImpl;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.BatchServiceBuilder;
import org.jboss.msc.service.ServiceActivatorContext;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.tm.JBossXATerminator;

/**
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano
 *         Maestri</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ConnectorSubsystemAdd extends AbstractSubsystemAdd<ConnectorSubsystemElement> {

    private static final long serialVersionUID = -874698675049495644L;
    private boolean archiveValidation = true;
    private boolean archiveValidationFailOnError = true;
    private boolean archiveValidationFailOnWarn = false;
    private boolean beanValidation = true;

    private String shortRunningThreadPool = null;
    private String longRunningThreadPool = null;

    protected ConnectorSubsystemAdd() {
        super(Namespace.CURRENT.getUriString());
    }

    @Override
    protected <P> void applyUpdate(final UpdateContext updateContext, final UpdateResultHandler<? super Void, P> resultHandler,
            final P param) {
        final BatchBuilder builder = updateContext.getBatchBuilder();

        new RaDeploymentActivator().activate(new ServiceActivatorContext() {
            public BatchBuilder getBatchBuilder() {
                return builder;
            }
        });

        WorkManager wm = new WorkManagerImpl();

        final WorkManagerService wmService = new WorkManagerService(wm);
        final BatchServiceBuilder<WorkManager> wmServiceBuilder = builder.addService(ConnectorServices.WORKMANAGER_SERVICE,
                wmService);
        wmServiceBuilder.addDependency(ThreadsServices.EXECUTOR.append(shortRunningThreadPool), Executor.class,
                wmService.getExecutorShortInjector());
        wmServiceBuilder.addDependency(ThreadsServices.EXECUTOR.append(longRunningThreadPool), Executor.class,
                wmService.getExecutorLongInjector());
        wmServiceBuilder.addDependency(TxnServices.JBOSS_TXN_XA_TERMINATOR, JBossXATerminator.class,
                wmService.getXaTerminatorInjector());
        wmServiceBuilder.setInitialMode(Mode.ACTIVE);

        CloneableBootstrapContext ctx = new BaseCloneableBootstrapContext();
        final DefaultBootStrapContextService defaultBootCtxService = new DefaultBootStrapContextService(ctx);
        final BatchServiceBuilder<CloneableBootstrapContext> defaultBootCtxServiceBuilder = builder.addService(
                ConnectorServices.DEFAULT_BOOTSTRAP_CONTEXT_SERVICE, defaultBootCtxService);
        defaultBootCtxServiceBuilder.addDependency(ConnectorServices.WORKMANAGER_SERVICE, WorkManager.class,
                defaultBootCtxService.getWorkManagerValueInjector());
        defaultBootCtxServiceBuilder.addDependency(TxnServices.JBOSS_TXN_XA_TERMINATOR, JBossXATerminator.class,
                defaultBootCtxService.getXaTerminatorInjector());
        defaultBootCtxServiceBuilder.addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER,
                com.arjuna.ats.jbossatx.jta.TransactionManagerService.class, defaultBootCtxService.getTxManagerInjector());

        defaultBootCtxServiceBuilder.setInitialMode(Mode.ACTIVE);

        final ConnectorSubsystemConfiguration config = new ConnectorSubsystemConfiguration();

        config.setArchiveValidation(archiveValidation);
        config.setArchiveValidationFailOnError(archiveValidationFailOnError);
        config.setArchiveValidationFailOnWarn(archiveValidationFailOnWarn);
        config.setBeanValidation(false);

        final ConnectorConfigService connectorConfigService = new ConnectorConfigService(config);
        final BatchServiceBuilder<ConnectorSubsystemConfiguration> configServiceBuilder = builder.addService(
                ConnectorServices.CONNECTOR_CONFIG_SERVICE, connectorConfigService);
        configServiceBuilder.addDependency(ConnectorServices.DEFAULT_BOOTSTRAP_CONTEXT_SERVICE,
                CloneableBootstrapContext.class, connectorConfigService.getDefaultBootstrapContextInjector());
        configServiceBuilder.setInitialMode(Mode.ACTIVE);
    }

    protected void applyUpdate(ConnectorSubsystemElement element) throws UpdateFailedException {
        element.setArchiveValidation(archiveValidation);
        element.setArchiveValidationFailOnError(archiveValidationFailOnError);
        element.setArchiveValidationFailOnWarn(archiveValidationFailOnWarn);
        element.setBeanValidation(false);
        element.setLongRunningThreadPool(longRunningThreadPool);
        element.setShortRunningThreadPool(shortRunningThreadPool);
    }

    protected ConnectorSubsystemElement createSubsystemElement() {
        ConnectorSubsystemElement element = new ConnectorSubsystemElement();
        element.setArchiveValidation(archiveValidation);
        element.setArchiveValidationFailOnError(archiveValidationFailOnError);
        element.setArchiveValidationFailOnWarn(archiveValidationFailOnWarn);
        element.setBeanValidation(false);
        element.setLongRunningThreadPool(longRunningThreadPool);
        element.setShortRunningThreadPool(shortRunningThreadPool);
        return element;
    }

    public boolean isArchiveValidation() {
        return archiveValidation;
    }

    public void setArchiveValidation(final boolean archiveValidation) {
        this.archiveValidation = archiveValidation;
    }

    public boolean isArchiveValidationFailOnError() {
        return archiveValidationFailOnError;
    }

    public void setArchiveValidationFailOnError(final boolean archiveValidationFailOnError) {
        this.archiveValidationFailOnError = archiveValidationFailOnError;
    }

    public boolean isArchiveValidationFailOnWarn() {
        return archiveValidationFailOnWarn;
    }

    public void setArchiveValidationFailOnWarn(final boolean archiveValidationFailOnWarn) {
        this.archiveValidationFailOnWarn = archiveValidationFailOnWarn;
    }

    public boolean isBeanValidation() {
        return false;
    }

    public void setBeanValidation(final boolean beanValidation) {
        this.beanValidation = beanValidation;
    }

    public String getShortRunningThreadPool() {
        return shortRunningThreadPool;
    }

    public void setShortRunningThreadPool(String shortRunningThreadPool) {
        this.shortRunningThreadPool = shortRunningThreadPool;
    }

    public String getLongRunningThreadPool() {
        return longRunningThreadPool;
    }

    public void setLongRunningThreadPool(String longRunningThreadPool) {
        this.longRunningThreadPool = longRunningThreadPool;
    }

}
