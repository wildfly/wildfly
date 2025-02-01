/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.coordinator.service;

import com.arjuna.ats.arjuna.recovery.RecoveryManager;
import com.arjuna.ats.arjuna.tools.osb.mbean.OSBTypeHandler;
import com.arjuna.ats.arjuna.tools.osb.mbean.ObjStoreBrowser;

import io.narayana.lra.coordinator.domain.model.FailedLongRunningAction;
import io.narayana.lra.coordinator.domain.model.LongRunningAction;
import io.narayana.lra.coordinator.internal.Implementations;
import io.narayana.lra.coordinator.internal.LRARecoveryModule;
import io.narayana.lra.coordinator.tools.osb.mbean.LRAActionBean;

import org.jboss.logging.Logger;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.microprofile.lra.coordinator._private.MicroProfileLRACoordinatorLogger;
import org.wildfly.security.manager.WildFlySecurityManager;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Supplier;

import static org.wildfly.extension.microprofile.lra.coordinator.service.LRACoordinatorService.CONTEXT_PATH;

public class LRARecoveryService implements Service {
    private static final Logger log = Logger.getLogger(LRARecoveryService.class);

    private final Supplier<ExecutorService> executorSupplier;

    private volatile LRARecoveryModule lraRecoveryModule;

    private ObjStoreBrowser objStoreBrowser;

    private static final String[][] LRA_OSB_TYPES = {
            // osTypeClassName, beanTypeClassName - see
            // com.arjuna.ats.arjuna.tools.osb.mbean.ObjStoreBrowser
            { LongRunningAction.getType().substring(1), LongRunningAction.class.getName(),
                    LRAActionBean.class.getName() },
            { FailedLongRunningAction.getType().substring(1), FailedLongRunningAction.class.getName(),
                    LRAActionBean.class.getName() } };

    public LRARecoveryService(Supplier<ExecutorService> executorSupplier) {
        this.executorSupplier = executorSupplier;
    }

    private void startRecoveryScan(final StartContext context) {
        assert executorSupplier != null;
        final ExecutorService service = executorSupplier.get();
        final Runnable task = () -> {
            try {
                // run an initial recovery scan - skip periodicWorkFirstPass since it is a no-op
                lraRecoveryModule.periodicWorkSecondPass();
                context.complete();
            } catch (Exception e) {
                MicroProfileLRACoordinatorLogger.LOGGER.failedToRunRecoveryScan(CONTEXT_PATH, e);
            }
        };
        try {
            service.execute(task);
        } catch (RejectedExecutionException e) {
            task.run();
        } finally {
            context.asynchronous();
        }
    }

    @Override
    public synchronized void start(final StartContext context) throws StartException {
        // installing LRA recovery types
        Implementations.install();

        // Add ObjStore type handlers
        objStoreBrowser = ObjStoreBrowser.getInstance();

        for (String[] typeAndBean : LRA_OSB_TYPES) {
            String typeName = typeAndBean[0].replace("/", File.separator);
            objStoreBrowser.addOSBTypeHandler(typeName, new OSBTypeHandler(true, true, typeAndBean[1], typeAndBean[2],
                    typeAndBean[0], null, this.getClass().getClassLoader()));
        }

        final ClassLoader loader = LRARecoveryService.class.getClassLoader();
        WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(loader);
        try {
            try {
                lraRecoveryModule = LRARecoveryModule.getInstance();
                // verify if the getInstance has added the module else could be removed meanwhile and adding explicitly
                if(RecoveryManager.manager().getModules().stream()
                    .noneMatch(rm -> rm instanceof LRARecoveryModule)) {
                    RecoveryManager.manager().addModule(lraRecoveryModule);
                }
                startRecoveryScan(context);
            } catch (Exception e) {
                throw MicroProfileLRACoordinatorLogger.LOGGER.lraRecoveryServiceFailedToStart();
            }
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged((ClassLoader) null);
        }
    }

    @Override
    public synchronized void stop(final StopContext context) {
        Implementations.uninstall();
        if (lraRecoveryModule != null) {
            final RecoveryManager recoveryManager = RecoveryManager.manager();
            try {
                recoveryManager.removeModule(lraRecoveryModule, false);
            } catch (Exception e) {
                log.debugf("Cannot remove LRA recovery module %s while stopping %s",
                    lraRecoveryModule.getClass().getName(), LRARecoveryService.class.getName());
            }
        }

    }
}