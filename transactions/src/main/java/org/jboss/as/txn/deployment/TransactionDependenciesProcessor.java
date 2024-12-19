/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.txn.deployment;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.annotation.CompositeIndex;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoader;

import jakarta.transaction.TransactionScoped;
import jakarta.transaction.Transactional;
import java.util.List;


/**
 * Looks for usage of the @Transactional Jakarta Contexts and Dependency Injection interceptor (JTA 1.2) or the @TransactionScoped Jakarta Contexts and Dependency Injection context (JTA 1.2)
 * and adds the org.jboss.jts module dependency if they are found.
 *
 * Also adds the transaction API to deployments
 *
 * @author Paul Robinson
 */
public class TransactionDependenciesProcessor implements DeploymentUnitProcessor {

    private static final String JTS_MODULE = "org.jboss.jts";
    private static final String TRANSACTION_API = "jakarta.transaction.api";
    private static final String TRANSACTION_CLIENT = "org.wildfly.transaction.client";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        final DeploymentUnit unit = phaseContext.getDeploymentUnit();
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        final ModuleSpecification moduleSpec = unit.getAttachment(Attachments.MODULE_SPECIFICATION);
        moduleSpec.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, TRANSACTION_API).setImportServices(true).build());
        moduleSpec.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, TRANSACTION_CLIENT).setImportServices(true).build());

        final CompositeIndex compositeIndex = unit.getAttachment(Attachments.COMPOSITE_ANNOTATION_INDEX);
        if (compositeIndex == null) {
            return;
        }

        final List<AnnotationInstance> transactionalAnnotations = compositeIndex.getAnnotations(DotName.createSimple(Transactional.class.getName()));
        final List<AnnotationInstance> transactionScopedAnnotations = compositeIndex.getAnnotations(DotName.createSimple(TransactionScoped.class.getName()));

        if (!transactionalAnnotations.isEmpty() || !transactionScopedAnnotations.isEmpty()) {
            addJTSModuleDependencyToDeployment(unit);
        }
    }

    private void addJTSModuleDependencyToDeployment(DeploymentUnit unit) {
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();
        final ModuleSpecification moduleSpec = unit.getAttachment(Attachments.MODULE_SPECIFICATION);
        moduleSpec.addSystemDependency(ModuleDependency.Builder.of(moduleLoader, JTS_MODULE).setImportServices(true).build());
    }
}
